package com.lmgy.redirect.ui.fragment

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import com.kyleduo.switchbutton.SwitchButton
import com.lmgy.redirect.R
import com.lmgy.redirect.base.BaseFragment
import com.lmgy.redirect.db.data.HostData
import com.lmgy.redirect.net.LocalVpnService
import com.lmgy.redirect.viewmodel.HostViewModel
import com.lmgy.redirect.viewmodel.HostViewModelFactory
import com.lmgy.redirect.viewmodel.Injection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * @author lmgy
 * @date 2019/8/29
 */
class HomeFragment : BaseFragment() {

    private var waitingForVPNStart: Boolean = false
    private lateinit var mContext: Context
    private lateinit var mBtnVpn: SwitchButton
    private lateinit var hostViewModelFactory: HostViewModelFactory
    private lateinit var viewModel: HostViewModel
    private lateinit var mView: View

    private val disposable = CompositeDisposable()
    private var hostList: MutableList<HostData> = mutableListOf()
    private val vpnStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (LocalVpnService.BROADCAST_VPN_STATE == intent.action && intent.getBooleanExtra("running", false)) {
                waitingForVPNStart = false
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this.context ?: requireContext()
        hostViewModelFactory = Injection.provideHostViewModelFactory(mContext)
        viewModel = ViewModelProvider(this, hostViewModelFactory).get(HostViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.fragment_home, container, false)
        initView(mView)
        return mView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    private fun initView(view: View) {
        mBtnVpn = view.findViewById(R.id.btn_vpn)
    }


    override fun onStop() {
        super.onStop()
        disposable.clear()
    }

    private fun checkHost() {
        mBtnVpn.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (hostList.size == 0) {
                    showDialog()
                } else {
                    startVPN()
                }
            } else {
                shutdownVPN()
            }
        }
        LocalBroadcastManager.getInstance(mContext).registerReceiver(vpnStateReceiver,
                IntentFilter(LocalVpnService.BROADCAST_VPN_STATE))
    }

    override fun initData() {
        disposable.add(viewModel.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    hostList = it
                    checkHost()
                })
    }

    override fun onResume() {
        super.onResume()
        setButton(!waitingForVPNStart && !LocalVpnService.isRunning())
    }

    private fun startVPN() {
        waitingForVPNStart = false
        val vpnIntent = LocalVpnService.prepare(mContext)
        if (vpnIntent != null) {
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE)
        } else {
            onActivityResult(VPN_REQUEST_CODE, Activity.RESULT_OK, null)
        }
    }

    private fun setButton(enable: Boolean) {
        mBtnVpn.isChecked = !enable
    }

    override fun checkStatus() {
        menu?.findItem(R.id.nav_home)?.isChecked = true
        toolbar?.title = getString(R.string.nav_home)
    }

    private fun showDialog() {
        val builder = AlertDialog.Builder(mContext)
        builder.setCancelable(false)
                .setTitle(getString(R.string.dialog_title))
                .setMessage(getString(R.string.dialog_message))
                .setPositiveButton(getString(R.string.dialog_confirm)) { _, _ ->
                    setButton(true)
                    Navigation.findNavController(mView).navigate(R.id.nav_rules)
                }
                .setNegativeButton(getString(R.string.dialog_cancel)) { _, _ -> setButton(true) }
                .show()
    }

    private fun shutdownVPN() {
        if (LocalVpnService.isRunning()) {
            mContext.startService(Intent(mContext, LocalVpnService::class.java).setAction(LocalVpnService.ACTION_DISCONNECT))
        }
        setButton(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            waitingForVPNStart = true
            mContext.startService(Intent(mContext, LocalVpnService::class.java).setAction(LocalVpnService.ACTION_CONNECT))
            setButton(false)
        }
    }

    companion object {
        const val VPN_REQUEST_CODE: Int = 0x0F
    }
}
