package com.pnlkc.set

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.pnlkc.set.data.DataSource
import com.pnlkc.set.data.DataSource.KEY_USED_CARD_LIST
import com.pnlkc.set.databinding.StartFragmentBinding
import com.pnlkc.set.model.SetViewModel

class StartFragment : Fragment() {

    private var _binding: StartFragmentBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SetViewModel by activityViewModels()

    private lateinit var backPressCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = StartFragmentBinding.inflate(inflater, container, false)
        val root: View = binding.root

        checkExistSaveGame()

        // 로티 애니메이션 재생 길이 제한
        binding.lottieAnimationView.setMaxFrame(80)

        // 새로 플레이하기 버튼
        binding.newGameBtn.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_setFragment)
            sharedViewModel.isContinueGame = false
        }

        binding.continueGameBtn.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_setFragment)
            sharedViewModel.isContinueGame = true
        }

        // ?(룰) 버튼튼
        binding.ruleBtn.setOnClickListener {
            findNavController().navigate(R.id.action_startFragment_to_setRuleFragment)
        }

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

        return root
    }

    // 저장된 게임이 있는지 확인
    private fun checkExistSaveGame() {
        val sharedPreferences =
            requireActivity().getSharedPreferences(DataSource.KEY_PREFS, Context.MODE_PRIVATE)

        if (sharedPreferences.contains(KEY_USED_CARD_LIST)) {
            binding.continueGameBtn.visibility = View.VISIBLE
            binding.newGameBtn.text = "새로 시작하기"
        } else {
            binding.continueGameBtn.visibility = View.INVISIBLE
            binding.newGameBtn.text = "세트 플레이하기"
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        backPressCallback.remove()
    }
}