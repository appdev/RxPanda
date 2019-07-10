package com.pandaq.sample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.pandaq.app_launcher.entites.ZhihuData
import com.pandaq.rxpanda.RxPanda
import com.pandaq.rxpanda.transformer.RxScheduler
import com.pandaq.rxpanda.utils.GsonUtil
import com.pandaq.sample.apis.ApiService
import com.pandaq.sample.apis.AppCallBack
import com.pandaq.sample.entities.ZooData
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val compositeDisposable: CompositeDisposable = CompositeDisposable()
    private val apiService = RxPanda.retrofit().create(ApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        normalData.setOnClickListener(this)
        newJsonKeyData.setOnClickListener(this)
        stringData.setOnClickListener(this)
        noShellData.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.normalData -> {
                apiService.zooList
                    .doOnSubscribe { t -> compositeDisposable.add(t) }
                    .compose(RxScheduler.sync())
                    .subscribe(object : AppCallBack<List<ZooData>>() {
                        override fun success(data: List<ZooData>?) {
                            dataString.text = GsonUtil.gson().toJson(data)
                        }

                        override fun fail(code: Long?, msg: String?) {
                            dataString.text = msg
                        }

                        override fun finish(success: Boolean) {

                        }

                    })
            }

            R.id.newJsonKeyData -> {
                apiService.newJsonKeyData()
                    .doOnSubscribe { t -> compositeDisposable.add(t) }
                    .compose(RxScheduler.sync())
                    .subscribe(object : AppCallBack<List<ZooData>>() {
                        override fun success(data: List<ZooData>?) {
                            dataString.text = GsonUtil.gson().toJson(data)
                        }

                        override fun fail(code: Long?, msg: String?) {
                            dataString.text = msg
                        }

                        override fun finish(success: Boolean) {

                        }

                    })
            }

            R.id.stringData -> {
                apiService.stringData()
                    .doOnSubscribe { t -> compositeDisposable.add(t) }
                    .compose(RxScheduler.sync())
                    .subscribe(object : AppCallBack<String>() {
                        override fun success(data: String) {
                            dataString.text = GsonUtil.gson().toJson(data)
                        }

                        override fun fail(code: Long?, msg: String?) {
                            dataString.text = msg
                        }

                        override fun finish(success: Boolean) {

                        }

                    })
            }

            R.id.noShellData -> {
                apiService.zhihu()
                    .doOnSubscribe { t -> compositeDisposable.add(t) }
                    .compose(RxScheduler.sync())
                    .subscribe(object : AppCallBack<ZhihuData>() {
                        override fun success(data: ZhihuData) {
                            dataString.text = GsonUtil.gson().toJson(data)
                        }

                        override fun fail(code: Long?, msg: String?) {
                            dataString.text = msg
                        }

                        override fun finish(success: Boolean) {

                        }

                    })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.dispose()
    }
}
