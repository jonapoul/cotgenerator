package com.jonapoul.cotbeacon.ui.chat

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jonapoul.cotbeacon.cot.ChatCursorOnTarget
import com.jonapoul.common.cot.CotTeam
import com.jonapoul.common.di.IBuildResources
import com.jonapoul.common.di.IUiResources
import com.jonapoul.common.prefs.CommonPrefs
import com.jonapoul.common.prefs.getStringFromPair
import com.jonapoul.common.presets.OutputPreset
import com.jonapoul.common.repositories.IDeviceUidRepository
import com.jonapoul.common.repositories.IStatusRepository
import com.jonapoul.common.service.ServiceState
import com.jonapoul.common.ui.viewBinding
import com.jonapoul.common.utils.Notify
import com.jonapoul.common.utils.Protocol
import com.jonapoul.cotbeacon.BeaconApplication
import com.jonapoul.cotbeacon.R
import com.jonapoul.cotbeacon.databinding.FragmentChatBinding
import com.jonapoul.cotbeacon.repositories.IChatRepository
import com.jonapoul.cotbeacon.ui.IChatServiceCommunicator
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class ChatFragment : Fragment(R.layout.fragment_chat) {

    @Inject
    lateinit var prefs: SharedPreferences

    @Inject
    lateinit var uiResources: IUiResources

    @Inject
    lateinit var buildResources: IBuildResources

    @Inject
    lateinit var chatRepository: IChatRepository

    @Inject
    lateinit var deviceUidRepository: IDeviceUidRepository

    @Inject
    lateinit var statusRepository: IStatusRepository

    private val binding by viewBinding(FragmentChatBinding::bind)

    private lateinit var adapter: ChatAdapter

    private val serviceCommunicator by lazy { requireActivity() as IChatServiceCommunicator }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate")
        setHasOptionsMenu(true)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated")
        /* Block notifications of new messages whilst this fragment is open */
        BeaconApplication.chatFragmentIsVisible = true

        /* Colour the hint message with the accent */
        binding.chatMessageInput.hintTextColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), uiResources.accentColourId))

        /* Bring the shaded box to the front, then have it intercept all touch events (when visible, of course) */
        binding.disabledBox.bringToFront()
        binding.disabledBox.setOnTouchListener { _, _ -> true }

        val context = requireContext()
        initialiseRecyclerView(context)
        initialiseSendButton(context)
        observeServiceStatus()
        observeChatStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("onDestroyView")
        /* Re-allow notifications of chat messages */
        BeaconApplication.chatFragmentIsVisible = false
    }

    private fun initialiseRecyclerView(context: Context) {
        Timber.d("initialiseRecyclerView")
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(context).also {
            /* When new data is added, put it at the bottom of the view */
            it.stackFromEnd = true
        }
        adapter = ChatAdapter(context).also {
            it.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    /* New item arrived, so scroll to the bottom to see it */
                    super.onChanged()
                    binding.chatRecyclerView.scrollToPosition(adapter.itemCount - 1)
                }
            })
        }
        binding.chatRecyclerView.adapter = adapter
    }

    private fun initialiseSendButton(context: Context) {
        Timber.d("initialiseSendButton")
        binding.chatSendButton.setBackgroundColor(ContextCompat.getColor(context, uiResources.accentColourId))
        val icon = ContextCompat.getDrawable(context, R.drawable.send)
        binding.chatSendButton.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
        binding.chatSendButton.setOnClickListener {
            getInputText()?.let { sendChat(it) }
        }
    }

    private fun sendChat(inputMessage: String) {
        Timber.d("sendChat")
        val chat = ChatCursorOnTarget(isIncoming = false, buildResources = buildResources).apply {
            uid = deviceUidRepository.getUid()
            messageUid = UUID.randomUUID().toString()
            team = CotTeam.fromPrefs(prefs)
            callsign = prefs.getStringFromPair(CommonPrefs.CALLSIGN)
            message = inputMessage
            outputPreset = OutputPreset.fromPrefs(prefs)
        }
        serviceCommunicator.sendChat(chat)
        binding.chatMessageInput.editText?.text?.clear()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun observeServiceStatus() {
        Timber.d("observeServiceStatus")
        statusRepository.getStatus().observe(viewLifecycleOwner) {
            Timber.d("Service state = %s", it)
            if (it == ServiceState.RUNNING) {
                binding.chatStatus.visibility = View.GONE
                binding.disabledBox.visibility = View.GONE
                val runningSsl = Protocol.fromPrefs(prefs) == Protocol.SSL
                binding.sslWarning.visibility = if (runningSsl) View.VISIBLE else View.GONE
            } else {
                binding.chatStatus.visibility = View.VISIBLE
                binding.disabledBox.visibility = View.VISIBLE
                binding.sslWarning.visibility = View.GONE
            }
        }
    }

    private fun observeChatStatus() {
        Timber.d("observeChatStatus")
        chatRepository.getChats().observe(viewLifecycleOwner) {
            Timber.d("Chats updated %s", it)
            adapter.updateChats(it)
        }
        chatRepository.getErrors().observe(viewLifecycleOwner) {
            Timber.d("Chat error received: %s", it)
            Notify.red(requireView(), "Chat error: $it")
        }
    }

    private fun getInputText(): String? {
        return binding.chatMessageInput.editText?.text?.toString()?.trim()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        Timber.d("onCreateOptionsMenu")
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
    }
}