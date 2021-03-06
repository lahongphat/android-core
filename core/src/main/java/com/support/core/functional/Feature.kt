package com.support.core.functional

import android.view.View
import androidx.lifecycle.*
import com.support.core.R
import com.support.core.extension.block

abstract class Feature {
    private var mView: View? = null
    val view: View? get() = mView
    val requireView: View
        get() = mView ?: error("Feature ${this.javaClass.simpleName} not attached to view yet!")
    open val self get() = this

    companion object {
        internal val TAG_ID = R.id.feature
    }

    internal open fun attach(view: View) {
        mView = view
        view.setTag(TAG_ID, this)
        onAttached(view)
    }

    internal open fun detach() = block(mView) {
        setTag(TAG_ID, null)
        onDetached()
        mView = null
    }

    protected open fun onAttached(view: View) {}
    protected open fun onDetached() {}
}

abstract class LifecycleFeature : Feature(), LifecycleOwner, ViewModelStoreOwner {

    private var mSourceOwner: LifecycleOwner? = null
    private var mLifecycleRegistry: LifecycleRegistry? = null
    private val registry get() = lifecycle as LifecycleRegistry
    private val mSourceObserver = LifecycleEventObserver { _, event ->

        if (event == Lifecycle.Event.ON_DESTROY) {
            detach()
        } else {
            when (event) {
                Lifecycle.Event.ON_START -> onStart()
                Lifecycle.Event.ON_STOP -> onStop()
                else -> {
                }
            }
            registry.handleLifecycleEvent(event)
        }
    }

    override fun getLifecycle(): Lifecycle {
        if (mLifecycleRegistry == null) mLifecycleRegistry = LifecycleRegistry(this)
        return mLifecycleRegistry!!
    }

    override fun getViewModelStore(): ViewModelStore {
        val owner = mSourceOwner ?: error("Owner has been detached")
        return (owner as? ViewModelStoreOwner)?.viewModelStore
            ?: error("${owner.javaClass.simpleName} is not ViewModelStoreOwner")
    }

    @Deprecated("unused", ReplaceWith("attach(view: View, owner: LifecycleOwner)"))
    override fun attach(view: View) {
        error("Not support")
    }

    override fun detach() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        mSourceOwner?.lifecycle?.removeObserver(mSourceObserver)
        super.detach()
        mLifecycleRegistry = null
        mSourceOwner = null
    }

    internal fun attach(view: View, owner: LifecycleOwner) {
        mSourceOwner = owner
        super.attach(view)
        owner.lifecycle.addObserver(mSourceObserver)
    }

    protected open fun onStart() {}
    protected open fun onStop() {}
}


fun View.set(feature: Feature) {
    val oldFeature = getTag(Feature.TAG_ID) as? Feature
    if (oldFeature == feature) return
    oldFeature?.detach()
    feature.attach(this)
}

fun View.set(owner: LifecycleOwner, feature: LifecycleFeature) {
    val oldFeature = getTag(Feature.TAG_ID) as? Feature
    if (oldFeature == feature) return
    oldFeature?.detach()
    feature.attach(this, owner)
}
