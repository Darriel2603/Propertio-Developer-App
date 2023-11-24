package com.propertio.developer.pesan

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.RecyclerView
import com.propertio.developer.R
import com.propertio.developer.api.Retro
import com.propertio.developer.api.common.message.MessageApi
import com.propertio.developer.api.common.message.MessageDetailResponse
import com.propertio.developer.databinding.ItemChatContainerBinding
import com.propertio.developer.model.Chat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


typealias onClickChat = (Chat) -> Unit
class ChatAdapter(
    private val context: Context,
    var chatList: MutableList<Chat>,
    private val onClickChat: onClickChat
) : RecyclerView.Adapter<ChatAdapter.ItemChatViewHolder>()
{
    inner class ItemChatViewHolder(
        private val binding: ItemChatContainerBinding
    ) : RecyclerView.ViewHolder(binding.root)
    {
        fun bind(data: Chat) {
            with(binding) {
                val date = dateFormat(data.time ?: "1970-01-01T05:18:39.000000Z")

                textSubject.text = data.subject
                textName.text = data.name
                textTime.text = date


                iconUnreadNotification.visibility = if (data.read == 0) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }


                itemView.setOnClickListener {
                    updateChat(data, bindingAdapterPosition)
                    onClickChat(data)
                }

            }
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemChatViewHolder {
        val binding = ItemChatContainerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false)
        return ItemChatViewHolder(binding)
    }

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: ItemChatViewHolder, position: Int) {
        holder.bind(chatList[position])
    }

    private fun dateFormat(date: String): String {
        val _date = date.split("T")
        val _time = _date[1].split(".")[0].split(":") // hh:mm:ss

        val dateString = _date[0].split("-") // yyyy-mm-dd
        val time = "${_time[0]}:${_time[1]}" // hh:mm

//        Log.d("ChatAdapter", "dateFormat: $dateString")
//        Log.d("ChatAdapter", "dateFormat: $time")

        // get today date
        val today = java.util.Calendar.getInstance()

        // get the delta between today and the date, if it's more than 1 day, then return the date. make sure it's on the same month and year
        Log.d("ChatAdapter Statement 1", "statement #1: ${today.get(java.util.Calendar.YEAR)} > ${dateString[0].toInt()}")
        Log.d("ChatAdapter Statement 2", "statement #2: ${today.get(java.util.Calendar.DAY_OF_MONTH)} == ${dateString[2].toInt()}  &&")
        Log.d("ChatAdapter Statement 3", "statement #2: ${today.get(java.util.Calendar.MONTH)} == ${dateString[1].toInt() - 1}")

        if (
            today.get(java.util.Calendar.YEAR) > dateString[0].toInt()
            )
        {
            Log.d("ChatAdapter", "dateFormat #1: ${dateString[0]}.${dateString[1]}.${dateString[2]}")

            return "${dateString[0]}.${dateString[1]}.${dateString[2]}"
        }
        else if (
            today.get(java.util.Calendar.DAY_OF_MONTH) == dateString[2].toInt()
            && today.get(java.util.Calendar.MONTH) == dateString[1].toInt() - 1
            )
        {
            Log.d("ChatAdapter", "dateFormat #2: $time")

            return time
        }

        val months = context.resources.getStringArray(R.array.list_of_months)

        Log.d("ChatAdapter", "dateFormat #3: ${dateString[2]} ${months[dateString[1].toInt() - 1]}")

        return "${dateString[2]} ${months[dateString[1].toInt() - 1]}"
    }

    @WorkerThread
    fun updateChat(data: Chat, bindingAdapterPosition: Int) {
        Log.d("ChatAdapter", "updateChat: ${data.id}")

        // API
        val sharedPreferences = context.getSharedPreferences("account_data", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)
        val retro = Retro(token).getRetroClientInstance().create(MessageApi::class.java)

        if (data.id != null) {
            retro.getDetailMessage(data.id).enqueue(object : Callback<MessageDetailResponse> {

                override fun onResponse(
                    call: Call<MessageDetailResponse>,
                    response: Response<MessageDetailResponse>
                ) {
                    if (response.isSuccessful) {
                        val updatedChat = response.body()
                        if (updatedChat != null) {
                            // Update the chat
                            data.read = updatedChat.data?.read
                            // Notify the adapter about the change
                            notifyItemChanged(bindingAdapterPosition)
                        }
                    }

                }

                override fun onFailure(
                    call: Call<MessageDetailResponse>,
                    t: Throwable
                ) {
                    // Handle failure
                }
            })
        }
    }
}

