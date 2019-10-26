/*
 * *
 *  * Created by Ahmed Elshaer on 10/26/19 4:17 AM
 *  * Copyright (c) 2019 . All rights reserved.
 *  * Last modified 10/26/19 3:37 AM
 *
 */

package com.ahmed3elshaer.geosquar.home

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.ahmed3elshaer.geosquar.R
import com.ahmed3elshaer.geosquar.common.Event
import com.ahmed3elshaer.geosquar.common.extensions.*
import com.ahmed3elshaer.geosquar.common.location.RxLocationExt
import com.ahmed3elshaer.geosquar.common.models.Venue
import com.google.android.material.snackbar.Snackbar
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
    private val rxLocation: RxLocationExt = RxLocationExt()
    private val compositeDisposable = CompositeDisposable()
    private val viewModel: HomeViewModel by inject()
    private lateinit var adapter: VenuesAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewModel.viewState.observe(this, Observer {
            render(it)
        })
        getLocation()
        initVenuesList()
        initModeChange()
    }

    private fun initModeChange() {
        switch_mode.isChecked = isRealtime()
        switch_mode.setOnCheckedChangeListener { buttonView, isChecked ->
            changeMode(isChecked)
            getLocation()
        }
    }

    private fun initVenuesList() {
        adapter = VenuesAdapter()
        recycler_venues.itemAnimator = DefaultItemAnimator()
        recycler_venues.addItemDecoration(DividerItemDecoration(this, RecyclerView.VERTICAL))
        recycler_venues.adapter = adapter
    }

    private fun render(event: Event<HomeViewState>) {
        event.getContentIfNotHandled()?.apply {
            renderLoading(isLoading)
            error?.let {
                error.printStackTrace()
                renderError(error)
                return
            }
            venues?.let {
                if (it.isEmpty())
                    renderEmptyState()
                else
                    renderVenues(it)

            }

        }
    }

    private fun renderVenues(venues: List<Venue>) {
        Log.d("newList", venues.toString())
        text_state.hide()
        recycler_venues.show()
        adapter.updateList(venues)
    }

    private fun renderEmptyState() {
        recycler_venues.hide()
        text_state.show()
        text_state.text = getString(R.string.empty)
    }

    private fun getLocation() {
        if (isNetworkAvailable()) {
            rxLocation.stopLocationUpdates()
            compositeDisposable.add(
                    rxLocation.locations(this, isRealtime())
                            .subscribe({ location ->
                                viewModel.exploreVenues(location, isRealtime())


                            },
                                    { error: Throwable ->
                                        renderError(error)
                                    })
            )
        } else
            viewModel.checkForCachedVenues()

    }

    private fun renderError(error: Throwable) {
        error.printStackTrace()
        recycler_venues.hide()
        text_state.show()
        text_state.text = getString(R.string.error)
        showMessage(error.message)
    }

    private fun showMessage(message: String?) {
        message?.let {
            val view = findViewById<View>(android.R.id.content) ?: return
            Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                    .show()
        }

    }

    private fun renderLoading(shouldLoad: Boolean) {
        if (shouldLoad)
            progress_loading.show()
        else
            progress_loading.hide()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        rxLocation.onActivityResult(requestCode, resultCode, data)

    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        rxLocation.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
