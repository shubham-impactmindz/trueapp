package com.app.truewebapp.ui.component.login

//import com.app.truewebapp.ui.component.dashboard.MainActivity
//import com.app.truewebapp.ui.component.otp.OtpVerifyActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.truewebapp.R
import com.app.truewebapp.databinding.ActivityLoginBinding
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var adapter: AuthPagerAdapter

//    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val viewpager = binding.viewPager
        val tabLayout = binding.tabLayout

        adapter = AuthPagerAdapter(this)
        viewpager.adapter = adapter

        // Sync TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewpager) { tab, position ->
            tab.text = if (position == 0) "Login" else "Register"
        }.attach()

        tabLayout.getTabAt(0)?.select()
        tabLayout.getTabAt(0)?.view?.setBackgroundResource(R.drawable.border_tab_primary)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.view?.setBackgroundResource(R.drawable.border_tab_primary)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.view?.setBackgroundResource(R.drawable.border_tab_light)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

    }

    private fun setupTabListeners() {
//        binding.btnOtp.setOnClickListener {
//            currentTab = "otp"
//            binding.txtLoginSts.text = getString(R.string.login_otp_msg)
//            updateTabUI(isOtpSelected = true)
//        }
//
//        binding.btnPassword.setOnClickListener {
//            currentTab = "pwd"
//            binding.txtLoginSts.text = getString(R.string.login_pwd_msg)
//            updateTabUI(isOtpSelected = false)
//        }
    }

    private fun setupLoginButton() {
//        binding.btnLogin.setOnClickListener {
//            if (currentTab == "otp") {
//                startActivity(Intent(this, OtpVerifyActivity::class.java))
//            } else {
//                if (isValid()) {
//                    loginViewModel.login(
//                        auth_key,
//                        binding.edtUserName.text.toString(),
//                        binding.edtPassword.text.toString()
//                    )
//                }
//            }
//        }
    }

//    private fun isValid(): Boolean {
//        return when {
//            binding.edtUserName.text.toString().isEmpty() -> {
//                showMessage("Please enter username")
//                false
//            }
//            binding.edtPassword.text.toString().isEmpty() -> {
//                showMessage("Please enter password")
//                false
//            }
//            else -> true
//        }
//    }

    private fun updateTabUI(isOtpSelected: Boolean) {
//        if (isOtpSelected) {
//            binding.btnOtp.setBackgroundResource(R.drawable.rounded_corner_selected)
//            binding.btnPassword.setBackgroundResource(R.drawable.rounded_corner_unselected)
//            binding.lytPwdBox.visibility = View.GONE
//            binding.btnLogin.text = "Next"
//            binding.btnOtp.setTextColor(ContextCompat.getColor(this, R.color.white))
//            binding.btnPassword.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
//        } else {
//            binding.btnOtp.setBackgroundResource(R.drawable.rounded_corner_unselected)
//            binding.btnPassword.setBackgroundResource(R.drawable.rounded_corner_selected)
//            binding.lytPwdBox.visibility = View.VISIBLE
//            binding.btnLogin.text = "Login"
//            binding.btnOtp.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
//            binding.btnPassword.setTextColor(ContextCompat.getColor(this, R.color.white))
//        }
    }

//    private fun initializeObservers() {
//        loginViewModel.loginResponse.observe(this, Observer {
//            it?.let {
//                if (it.status == "success") {
//                    val preferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
//                    preferences.edit().apply {
//                        putString("login_key", it.data?.login_key)
//                        putString("EmployeeName", it.data?.user_data?.getOrNull(0)?.EmployeeName)
//                        putString("EmployeeImage", it.data?.user_data?.getOrNull(0)?.EmployeeImage)
//                        putString("EmailId", it.data?.user_data?.getOrNull(0)?.EmailId)
//                        apply()
//                    }
//
////                    startActivity(Intent(this, MainActivity::class.java))
////                    finish()
//                } else {
//                    showMessage(it.message)
//                }
//            }
//        })
//
//        loginViewModel.isLoading.observe(this, Observer {
////            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
//        })
//
//        loginViewModel.apiError.observe(this, Observer {
//            Log.e("apiError", "Error: $it")
//            it?.let { showMessage(it) }
//        })
//
//        loginViewModel.onFailure.observe(this, Observer {
//            Log.e("onFailure", "Error: ${it.message}")
//            it?.let { showMessage(ApiFailureTypes().getFailureMessage(it, this)) }
//        })
//    }

    private fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
