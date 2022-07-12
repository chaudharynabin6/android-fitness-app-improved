package com.androiddevs.runningappyt.adapters

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.androiddevs.runningappyt.databinding.ItemRunBinding
import com.androiddevs.runningappyt.db.Run
import com.androiddevs.runningappyt.utils.TimeFormatterUtil
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

//https://stackoverflow.com/questions/60423596/how-to-use-viewbinding-in-a-recyclerview-adapter
class RunAdapter : RecyclerView.Adapter<RunAdapter.RunViewHolder>() {

    inner class RunViewHolder(private val binding: ItemRunBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(run: Run){
            itemView.rootView.apply {
                Glide.with(this).load(run.img).into(
                    binding.ivRunImage
                )
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = run.timestamp
                }

                val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
                binding.tvDate.text = dateFormat.format(calendar.time)

                val avgSpeed = "${run.avgSpeedInKMH}km/hr"
                binding.tvAvgSpeed.text = avgSpeed

                val distanceInKm = "${run.distanceInMeters / 1000f}km"
                binding.tvDistance.text = distanceInKm

                binding.tvTime.text = TimeFormatterUtil.getFormattedStopWatchTime(run.timeInMillis)

                val caloriesBurned = "${run.caloriesBurned}kcal"
                binding.tvCalories.text = caloriesBurned
            }
        }
    }

    private val diffCallBack = object : DiffUtil.ItemCallback<Run>() {
        override fun areItemsTheSame(oldItem: Run, newItem: Run): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Run, newItem: Run): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }
    private val differ = AsyncListDiffer(
        this,
        diffCallBack
    )

    fun submitList(list: List<Run>) = differ.submitList(list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RunViewHolder {
        val binding = ItemRunBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return RunViewHolder(
            binding = binding
        )
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onBindViewHolder(holder: RunViewHolder, position: Int) {
        val run = differ.currentList[position]
        holder.bind(run = run)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}