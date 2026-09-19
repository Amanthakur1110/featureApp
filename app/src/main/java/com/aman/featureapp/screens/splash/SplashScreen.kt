package com.aman.featureapp.screens.splash


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel
import com.aman.featureapp.R


@Composable
@Preview
fun SplashScreen(viewmodel: SplashViewModel = viewModel()){

    Scaffold()

     {
        innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .background(color = Color.White, shape = RoundedCornerShape(0.dp)),
            contentAlignment = Alignment.Center
        ){
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.feature_fill),
                    contentDescription = "splash_logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(80.dp)

                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Feature App", fontSize = 20.sp, fontWeight = FontWeight.Bold)

                if(viewmodel.message!=""){
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(viewmodel.message,color= Color.LightGray)
                }
            }


            Column(
                modifier = Modifier.fillMaxSize().padding(bottom = 10.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Made in India \uD83C\uDDEE\uD83C\uDDF3", color = Color.LightGray)

            }



        }



    }



}