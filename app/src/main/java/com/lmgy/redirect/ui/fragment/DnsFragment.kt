package com.lmgy.redirect.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.lmgy.redirect.R
import com.lmgy.redirect.base.BaseFragment
import com.lmgy.redirect.db.data.DnsData
import com.lmgy.redirect.event.MessageEvent
import com.lmgy.redirect.net.LocalVpnService
import com.lmgy.redirect.viewmodel.DnsViewModel
import com.lmgy.redirect.viewmodel.DnsViewModelFactory
import com.lmgy.redirect.viewmodel.Injection
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus

/**
 * @author lmgy
 * @date 2019/8/29
 */
class DnsFragment : BaseFragment() {

    private lateinit var spinner: Spinner
    private lateinit var btnSave: Button
    private lateinit var ipv4: AppCompatEditText
    private lateinit var ipv6: AppCompatEditText
    private lateinit var mContext: Context
    private lateinit var dnsViewModelFactory: DnsViewModelFactory
    private lateinit var viewModel: DnsViewModel

    private var position = -1
    private var dnsList: MutableList<DnsData> = mutableListOf()
    private val disposable = CompositeDisposable()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this.context ?: requireContext()
        dnsViewModelFactory = Injection.provideDnsViewModelFactory(mContext)
        viewModel = ViewModelProvider(this, dnsViewModelFactory).get(DnsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_dns, container, false)
        initView(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initData()
    }

    override fun onStop() {
        super.onStop()
        disposable.clear()
    }

    private fun showDns() {
        if (dnsList.isNotEmpty()) {
            spinner.setSelection(dnsList[0].position)
            ipv4.setText(dnsList[0].ipv4)
            ipv6.setText(dnsList[0].ipv6)
        }

        val ipv4Array = resources.getStringArray(R.array.dns_ipv4_address)
        val ipv6Array = resources.getStringArray(R.array.dns_ipv6_address)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View,
                                        pos: Int, id: Long) {
                position = pos
                if (pos == 0) {
                    if (dnsList.isNotEmpty() && dnsList[0].position == 0) {
                        ipv4.setText(dnsList[0].ipv4)
                        ipv6.setText(dnsList[0].ipv6)
                        return
                    }
                    ipv4.setText("")
                    ipv6.setText("")
                } else {
                    ipv4.setText(ipv4Array[pos])
                    ipv6.setText(ipv6Array[pos])
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnSave.setOnClickListener {
            val tempIpv4 = (ipv4.text?.toString() ?: "").trim()
            val tempIpv6 = (ipv6.text?.toString() ?: "").trim()

            if (tempIpv4.isEmpty() || tempIpv6.isEmpty()) {
                EventBus.getDefault().post(MessageEvent(1, getString(R.string.empty)))
            } else {
                if (dnsList.isEmpty()) {
                    disposable.add(viewModel.insertDns(DnsData(position, tempIpv4, tempIpv6))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                goBack()
                            })
                } else {
                    disposable.add(viewModel.update(DnsData(dnsList[0].id, position, tempIpv4, tempIpv6))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                goBack()
                            })
                }
            }
        }
    }

    private fun goBack() {
        if (LocalVpnService.isRunning()) {
            EventBus.getDefault().post(MessageEvent(1, getString(R.string.save_successful_restart)))
        } else {
            EventBus.getDefault().post(MessageEvent(1, getString(R.string.save_successful)))
        }
        Navigation.findNavController(view!!).popBackStack()
    }

    override fun initData() {
        disposable.add(viewModel.getAllDns()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    dnsList = it
                    showDns()
                })
    }

    private fun initView(view: View) {
        spinner = view.findViewById(R.id.spinner)
        btnSave = view.findViewById(R.id.btn_save)
        ipv4 = view.findViewById(R.id.ipv4)
        ipv6 = view.findViewById(R.id.ipv6)
    }

    override fun checkStatus() {
        menu?.findItem(R.id.nav_dns)?.isChecked = true
        toolbar?.setTitle(R.string.nav_dns)
    }

}
