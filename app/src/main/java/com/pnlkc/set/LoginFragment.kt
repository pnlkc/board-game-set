package com.pnlkc.set

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.pnlkc.set.databinding.LoginFragmentBinding
import com.pnlkc.set.util.App

class LoginFragment : Fragment() {

    private var _binding: LoginFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var backPressCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = LoginFragmentBinding.inflate(inflater, container, false)

        // OnBackPressedCallback (익명 클래스) 객체 생성
        backPressCallback = object : OnBackPressedCallback(true) {
            // 뒤로가기 했을 때 실행되는 기능
            var backWait: Long = 0
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - backWait >= 2000) {
                    backWait = System.currentTimeMillis()
                    Toast.makeText(context,
                        resources.getString(R.string.back_btn_twice),
                        Toast.LENGTH_SHORT).show()
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

        autoLogin()

        // deprecated 대처
        // startActivityForResult => registerForActivityResult(객체).launch(intent)
        // onActivityResult() => registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        val gsoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                // 구글 로그인 결과 처리
                if (it.resultCode == Activity.RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
                    try {
                        val account = task.result
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        App.auth.signInWithCredential(credential)
                            .addOnCompleteListener(requireActivity()) { signInTask ->
                                if (signInTask.isSuccessful) {
                                    // 구글 로그인 성공
                                    moveMainMenuFragment()
                                } else {
                                    // 구글 로그인 실패
                                    Toast.makeText(requireContext(),
                                        resources.getString(R.string.login_failed),
                                        Toast.LENGTH_SHORT).show()
                                }
                            }
                    } catch (e: ApiException) {
                        Toast.makeText(requireContext(),
                            resources.getString(R.string.login_failed),
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }

        // 구글 로그인 버튼
        binding.googleLoginBtn.setOnClickListener {
            // 구글 로그인
            val gso = GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                //R.string.default_web_client_id 에러시 project 수준의 classpath ...google-services 버전 확인
                .requestIdToken(getString(R.string.default_web_client_id))
                .build()
            val signInIntent = GoogleSignIn.getClient(requireContext(), gso).signInIntent
            gsoLauncher.launch(signInIntent)
        }

        binding.guestLoginBtn.setOnClickListener {
            App.auth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    moveMainMenuFragment()
                } else {
                    Toast.makeText(requireContext(),
                        resources.getString(R.string.login_failed),
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun autoLogin() {
        if (App.checkAuth()) {
            moveMainMenuFragment()
        }
    }

    private fun moveMainMenuFragment() {
        findNavController().navigate(R.id.action_loginFragment_to_mainMenuFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        backPressCallback.remove()
    }
}