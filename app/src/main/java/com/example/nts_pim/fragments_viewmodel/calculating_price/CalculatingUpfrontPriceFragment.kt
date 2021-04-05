package com.example.nts_pim.fragments_viewmodel.calculating_price

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation

import com.example.nts_pim.R
import kotlinx.android.synthetic.main.calculating_upfront_price_fragment.*

class CalculatingUpfrontPriceFragment : Fragment() {
    val currentFragmentId = R.id.calculatingUpfrontPriceFragment

    private lateinit var viewModel: CalculatingUpfrontPriceViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.calculating_upfront_price_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        startProgressBar()
        toUpFrontPriceDetail()

    }

    private fun startProgressBar(){
        if(!get_upfront_price_progressBar.isAnimating){
            get_upfront_price_progressBar.animate()
        }
    }
    //Navigation
    private fun toUpFrontPriceDetail(){
        val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
        if (navController.currentDestination?.id == currentFragmentId){
            navController.navigate(R.id.action_calculatingUpfrontPriceFragment_to_upFrontPriceDetailFragment)
        }
    }

}
