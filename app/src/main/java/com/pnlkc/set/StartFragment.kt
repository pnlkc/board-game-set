package com.pnlkc.set

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.pnlkc.set.data.DataSource
import com.pnlkc.set.data.DataSource.KEY_SHUFFLED_CARD_LIST
import com.pnlkc.set.databinding.StartFragmentBinding
import com.pnlkc.set.model.SetViewModel

class StartFragment : Fragment() {

    private var _binding: StartFragmentBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SetViewModel by activityViewModels()

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var backPressCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = StartFragmentBinding.inflate(inflater, container, false)

        // OnBackPressedCallback (익명 클래스) 객체 생성
        backPressCallback = object : OnBackPressedCallback(true) {
            // 뒤로가기 했을 때 실행되는 기능
            var backWait: Long = 0
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - backWait >= 2000) {
                    backWait = System.currentTimeMillis()
                    Toast.makeText(context, "뒤로가기 버튼을 한번 더 누르면 종료됩니다", Toast.LENGTH_SHORT).show()
                } else {
                    activity?.finish()
                }
            }
        }
        // 액티비티의 BackPressedDispatcher에 여기서 만든 callback 객체를 등록
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressCallback)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences =
            requireActivity().getSharedPreferences(DataSource.KEY_PREFS, Context.MODE_PRIVATE)

        checkExistSaveGame()

        // 로티 애니메이션 재생 길이 제한
        binding.lottieAnimationView.setMaxFrame(80)

        // 새로 플레이하기 버튼
        binding.newGameBtn.setOnClickListener {
            if (sharedPreferences.contains(KEY_SHUFFLED_CARD_LIST)) {
                showDialog()
            } else {
                findNavController().navigate(R.id.action_startFragment_to_setFragment)
                sharedViewModel.isContinueGame = false
            }
        }

        // 이어하기 버튼
        binding.continueGameBtn.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_setFragment)
            sharedViewModel.isContinueGame = true
        }

        // ?(룰) 버튼튼
        binding.ruleBtn.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_setRuleFragment)
        }
    }

    // 저장된 게임이 있는지 확인
    private fun checkExistSaveGame() {
        if (sharedPreferences.contains(KEY_SHUFFLED_CARD_LIST)) {
            binding.continueGameBtn.visibility = View.VISIBLE
            binding.newGameBtn.text = "새로 시작하기"
        } else {
            binding.continueGameBtn.visibility = View.INVISIBLE
            binding.newGameBtn.text = "세트 플레이하기"
        }
    }

    private fun showDialog() {
        // 커스텀 Dialog 만들기
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.end_dialog)
        dialog.setCancelable(false)
        dialog.show()

        // Dialog 레이아웃의 뷰를 변수와 연결하기
        // 그냥 연결이 안되서 dialog 변수를 따로 만들고 거기서 findViewById해서 찾음
        val dialogTitleTextView = dialog.findViewById<TextView>(R.id.dialog_title_textview)
        val dialogTextView = dialog.findViewById<TextView>(R.id.dialog_textview)
        val dialogLeftBtn = dialog.findViewById<TextView>(R.id.dialog_left_btn)
        val dialogRightBtn = dialog.findViewById<TextView>(R.id.dialog_right_btn)

        // Dialog 뷰 기능 구현
        dialogTitleTextView.text = "새로 시작하시겠습니까?"
        dialogTextView.text = "게임을 새로 시작하면\n저장된 데이터가 사라집니다"
        dialogLeftBtn.apply {
            text = "아니오"
            setOnClickListener { dialog.dismiss() }
        }
        dialogRightBtn.apply {
            text = "예"
            setOnClickListener {
                findNavController().navigate(R.id.action_startFragment_to_setFragment)
                sharedViewModel.isContinueGame = false
                dialog.dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        backPressCallback.remove()
    }
}