package demo.xm.com.demo.down3.test

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import demo.xm.com.demo.R
import demo.xm.com.demo.down3.DownManager
import demo.xm.com.demo.down3.task.DownTasker

class DownAdapter(var downManager: DownManager?, var data: ArrayList<Any>? = ArrayList()) : RecyclerView.Adapter<DownViewHolder>() {

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): DownViewHolder {
        val view = LayoutInflater.from(p0.context).inflate(R.layout.item_down, p0, false)
        return DownViewHolder(view)
    }

    override fun getItemCount(): Int {
        return if (data?.isEmpty()!!) {
            0
        } else {
            data?.size!!
        }
    }

    override fun onBindViewHolder(p0: DownViewHolder, p1: Int) {
        p0.bind(data?.get(p1) as DownTasker, downManager)
    }
}