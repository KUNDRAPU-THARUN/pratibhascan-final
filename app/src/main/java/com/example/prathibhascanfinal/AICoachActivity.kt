package com.example.prathibhascanfinal

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.result.contract.ActivityResultContracts
import com.example.prathibhascanfinal.databinding.ActivityAiCoachBinding
import com.example.prathibhascanfinal.databinding.ItemChatAiBinding
import com.example.prathibhascanfinal.databinding.ItemChatUserBinding
import com.example.prathibhascanfinal.ui.base.BaseActivity
import kotlinx.coroutines.launch

class AICoachActivity : BaseActivity() {

    private lateinit var binding: ActivityAiCoachBinding
    override val viewModel: AICoachViewModel by viewModels()
    private lateinit var adapter: ChatAdapter
    private var selectedAttachmentUri: android.net.Uri? = null
    private var selectedBitmap: android.graphics.Bitmap? = null
    private var enrollment: SportEnrollment? = null
    private lateinit var sportName: String

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        uri?.let {
            selectedAttachmentUri = it
            try {
                val inputStream = contentResolver.openInputStream(it)
                selectedBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                showAttachmentPreview(it)
            } catch (_: Exception) {
                Toast.makeText(this, "Failed to load media", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiCoachBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupEdgeToEdge(binding.root)

        enrollment = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("ENROLLMENT_DATA", SportEnrollment::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("ENROLLMENT_DATA")
        }
        sportName = enrollment?.sportName ?: intent.getStringExtra("SPORT_NAME") ?: "General"
        
        findViewById<TextView>(R.id.tv_ai_coach_title)?.text = getString(R.string.ai_coach_title_format, sportName)

        initViews()
        observeState()
    }

    private fun initViews() {
        adapter = ChatAdapter()
        binding.rvChat.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvChat.adapter = adapter

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if ((text.isNotEmpty() || selectedBitmap != null)) {
                viewModel.sendMessage(text, selectedBitmap)
                binding.etMessage.text.clear()
                clearAttachment()
            }
        }

        binding.btnAttach.setOnClickListener {
            pickMedia.launch("image/* video/*")
        }

        binding.btnBackAi.setOnClickListener { finish() }
    }

    private fun showAttachmentPreview(uri: android.net.Uri) {
        binding.hsvAttachments.visibility = View.VISIBLE
        binding.containerAttachments.removeAllViews()
        
        val preview = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(100.dpToPx(), 100.dpToPx()).apply {
                setMargins(0, 0, 8.dpToPx(), 0)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageURI(uri)
            setOnClickListener { clearAttachment() }
        }
        binding.containerAttachments.addView(preview)
    }

    private fun clearAttachment() {
        selectedAttachmentUri = null
        selectedBitmap = null
        binding.hsvAttachments.visibility = View.GONE
        binding.containerAttachments.removeAllViews()
    }

    private fun Int.dpToPx() = (this * resources.displayMetrics.density).toInt()

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.chatHistory)
                    if (state.chatHistory.isNotEmpty()) {
                        binding.rvChat.post {
                            binding.rvChat.smoothScrollToPosition(state.chatHistory.size - 1)
                        }
                    }
                    
                    state.sportName?.let {
                        binding.tvAiCoachTitle.text = getString(R.string.ai_coach_title_format, it)
                    }
                    
                    binding.progressAi.visibility = if (state.accuracy == -1) View.VISIBLE else View.GONE
                    
                    updateSuggestions(state.suggestedQuestions)
                }
            }
        }
    }

    private fun updateSuggestions(suggestions: List<String>) {
        if (suggestions.isEmpty()) {
            binding.hsvSuggestions.visibility = View.GONE
            return
        }
        binding.hsvSuggestions.visibility = View.VISIBLE
        binding.containerSuggestions.removeAllViews()
        suggestions.forEach { suggestion ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = suggestion
                setTextColor(ContextCompat.getColor(context, R.color.white))
                setChipBackgroundColorResource(R.color.card_bg_light)
                setOnClickListener { viewModel.sendMessage(suggestion) }
            }
            binding.containerSuggestions.addView(chip)
        }
    }

    inner class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var items = listOf<ChatMessage>()

        fun submitList(newItems: List<ChatMessage>) {
            val oldSize = items.size
            items = newItems
            if (newItems.size > oldSize) {
                notifyItemRangeInserted(oldSize, newItems.size - oldSize)
            } else if (newItems.size < oldSize) {
                notifyItemRangeRemoved(0, oldSize)
                notifyItemRangeInserted(0, newItems.size)
            } else {
                notifyItemRangeChanged(0, newItems.size)
            }
        }

        override fun getItemViewType(position: Int): Int = if (items[position].isUser) 1 else 2

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == 1) {
                UserViewHolder(ItemChatUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            } else {
                AIViewHolder(ItemChatAiBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]
            if (holder is UserViewHolder) {
                holder.binding.tvMessageUser.text = item.text
            } else if (holder is AIViewHolder) {
                holder.binding.tvMessageAi.text = item.text
                if (item.text.contains("trouble connecting")) {
                    holder.binding.tvMessageAi.append("\n\n(Tap to retry)")
                    holder.binding.root.setOnClickListener { viewModel.retryLastMessage() }
                } else {
                    holder.binding.root.setOnClickListener(null)
                }
            }
        }

        override fun getItemCount(): Int = items.size

        inner class UserViewHolder(val binding: ItemChatUserBinding) : RecyclerView.ViewHolder(binding.root)
        inner class AIViewHolder(val binding: ItemChatAiBinding) : RecyclerView.ViewHolder(binding.root)
    }
}
