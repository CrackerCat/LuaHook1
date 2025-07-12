package com.kulipai.luahook.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.kulipai.luahook.R

class LogAdapter(private var logs: MutableList<String>) :
    RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    inner class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.card)
        val timeText: TextView = itemView.findViewById(R.id.time)
        val logText: TextView = itemView.findViewById(R.id.log)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false) // 加载自定义布局
        return LogViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.timeText.text = logs[position].substringBefore(".")
        holder.logText.text = logs[position].substringAfter("LuaXposed: ")
    }

    override fun getItemCount(): Int {
        return logs.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateLogs(newLogs: MutableList<String>) {
        logs = newLogs
        notifyDataSetChanged() // 通知 RecyclerView 更新数据
    }
}