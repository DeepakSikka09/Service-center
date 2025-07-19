package com.servicecenter.l2validation.app.ui.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.imageview.ShapeableImageView
import com.servicecenter.l2validation.R
import com.servicecenter.l2validation.databinding.AdapterProductImageBinding

class ProductImagePagerAdapter(
    private val mContext: Context,
    private val productImageList: ArrayList<String>
) : PagerAdapter() {
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val itemSlideBinding = AdapterProductImageBinding.inflate(LayoutInflater.from(mContext))
        setProductImages(itemSlideBinding.imageView, productImageList.get(position))
        container.addView(itemSlideBinding.root)
        return itemSlideBinding.root
    }

    private fun setProductImages(imageView: ShapeableImageView, product_image_url: String) {
        Glide.with(imageView)
            .load(product_image_url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.placeholder)
            .into(imageView)
    }

    override fun getCount(): Int {
        return productImageList.size
    }

    override fun isViewFromObject(view: View, obj: Any): Boolean {
        return view === obj
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val view = `object` as View
        container.removeView(view)
    }

    fun updateProductImages(productImage: List<String>) {
        productImageList.clear()
        productImageList.addAll(productImage)
        notifyDataSetChanged()
    }
}
