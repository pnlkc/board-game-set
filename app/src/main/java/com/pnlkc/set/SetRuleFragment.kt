package com.pnlkc.set

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
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.pnlkc.set.databinding.SetRuleFragmentBinding
import com.pnlkc.set.util.CustomFragment

class SetRuleFragment : CustomFragment() {

    private var _binding: SetRuleFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var backPressCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = SetRuleFragmentBinding.inflate(inflater, container, false)

        // OnBackPressedCallback (익명 클래스) 객체 생성
        backPressCallback = object : OnBackPressedCallback(true) {
            // 뒤로가기 했을 때 실행되는 기능
            override fun handleOnBackPressed() {
                isForcedExit = false
                findNavController().navigate(R.id.action_setRuleFragment_pop)
            }
        }
        // 액티비티의 BackPressedDispatcher에 여기서 만든 callback 객체를 등록
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressCallback)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로가기 버튼이 눌렸을 때
        binding.backBtn.setOnClickListener {
            isForcedExit = false
            findNavController().navigate(R.id.action_setRuleFragment_pop)
        }

        // 게임시작 버튼이 눌렸을 때
        binding.gameStartBtn.setOnClickListener {
            isForcedExit = false
            findNavController().navigate(R.id.action_setRuleFragment_to_setSinglePlayFragment)
        }

        binding.goMainMenuBtn.setOnClickListener {
            isForcedExit = false
            findNavController().navigate(R.id.action_setRuleFragment_pop)
        }

        binding.ruleOne.text = highlightString(getString(R.string.rule_one), "4가지 속성")
        binding.ruleTwo.text = highlightString(getString(R.string.rule_two), "모양, 색상, 개수, 음영")
        binding.ruleThree.text = highlightString(getString(R.string.rule_three), "3개의 유형")
        binding.ruleFour.text = highlightString(getString(R.string.rule_four), "12장의 카드")
        binding.ruleFive.text = highlightString(getString(R.string.rule_five), "모두 같거나 다른")
        binding.ruleSix.text = highlightString(getString(R.string.rule_six), "두장만 같으면 안됩니다")
        binding.ruleEightSinglePlay.text = highlightString(getString(R.string.rule_eight_single_play), "카드섞기 버튼")
        binding.ruleEightMultiPlay.text = highlightString(getString(R.string.rule_eight_multi_play), "Ⓧ 버튼을 누르면")

    }


    private fun highlightString(fullText: String, specificWord: String): SpannableString {
        // TextView 특정 부분 강조해서 보여주는 코드
        val spannableString = SpannableString(fullText)
        val start = fullText.indexOf(specificWord)
        val end = start + specificWord.length
        spannableString.setSpan(ForegroundColorSpan(ContextCompat
            .getColor(requireContext(), R.color.rule_highlight_text)),
            start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(StyleSpan(Typeface.BOLD),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(RelativeSizeSpan(1.3f),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        return spannableString
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        backPressCallback.remove()
    }
}