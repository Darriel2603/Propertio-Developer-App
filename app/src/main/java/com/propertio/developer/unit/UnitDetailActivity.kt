package com.propertio.developer.unit

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.webkit.WebView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.propertio.developer.NumericalUnitConverter
import com.propertio.developer.R
import com.propertio.developer.TokenManager
import com.propertio.developer.api.DomainURL
import com.propertio.developer.api.Retro
import com.propertio.developer.api.developer.DeveloperApi
import com.propertio.developer.api.developer.unitmanagement.UnitDetailResponse
import com.propertio.developer.carousel.CarouselAdapter
import com.propertio.developer.carousel.ImageData
import com.propertio.developer.databinding.ActivityUnitDetailBinding
import com.propertio.developer.project.ProjectDetailActivity
import com.propertio.developer.project.ProjectDetailActivity.Companion.PROJECT_DETAIL_PID
import com.propertio.developer.project.ProjectDetailActivity.Companion.PROJECT_DETAIL_UID
import com.propertio.developer.project.form.ProjectFormActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.regex.Pattern

class UnitDetailActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityUnitDetailBinding.inflate(layoutInflater)
    }

    private var unitId : Int = 0
    private var projectId : Int = 0

    private lateinit var carouselAdapter: CarouselAdapter
    private val carouselList = ArrayList<ImageData>()
    private lateinit var dots : ArrayList<TextView>

    private val developerApi by lazy {
        Retro(TokenManager(this).token)
            .getRetroClientInstance()
            .create(DeveloperApi::class.java)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.toolbarContainer.textViewTitle.text = "Detail Unit"

        binding.floatingButtonBack.setOnClickListener {
            Log.d("UnitDetailActivity", "onCreate: back")
            finish()
        }

        // Check if is it valid unit id
        unitId = intent.getIntExtra(PROJECT_DETAIL_UID, 0)
        projectId = intent.getIntExtra(PROJECT_DETAIL_PID, 0)
        Log.d("UnitDetailActivity", "onCreate: unitId $unitId")
        intent.removeExtra(PROJECT_DETAIL_UID)
        intent.removeExtra(PROJECT_DETAIL_PID)
        if (unitId == 0 && projectId == 0) {
            Log.w("UnitDetailActivity", "onCreate: unitId is 0")
            finish()
        }

        lifecycleScope.launch {
            fecthData()
        }

    }

    private suspend fun fecthData() {
        developerApi.getUnitDetail(projectId ,unitId).enqueue(object : Callback<UnitDetailResponse> {
            override fun onResponse(
                call: Call<UnitDetailResponse>,
                response: Response<UnitDetailResponse>
            ) {
                if (response.isSuccessful) {
                    val data = response.body()?.data
                    Log.d("UnitDetailActivity", "onResponse: $data")

                    if (data != null) {
                        lifecycleScope.launch {
                            loadData(data)

                            if (data.unitPhotos.isNullOrEmpty()) {
                                Log.w("UnitDetailActivity", "projectPhotos is null")
                            } else {
                                binding.viewPagerCarousel.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                                    override fun onPageSelected(position: Int) {
                                        selectedDot(position)
                                        super.onPageSelected(position)
                                    }
                                })

                                carouselAdapter = CarouselAdapter(carouselList)
                                binding.viewPagerCarousel.adapter = carouselAdapter
                                dots = ArrayList<TextView>()
                                setIndicator()

                            }
                        }

                    }
                }
            }

            override fun onFailure(call: Call<UnitDetailResponse>, t: Throwable) {
                Log.e("UnitDetailActivity", "onFailure: ${t.message}")
            }

        })

    }

    private suspend fun loadData(data: UnitDetailResponse.Unit) {
        loadTextBased(data)
        withContext(Dispatchers.IO) {
            loadImage(data.unitPhotos)
        }
        withContext(Dispatchers.Main) {
            Log.d("UnitDetailActivity", "loadVideo: ${data.unitVideo?.linkVideoURL}")
            loadVideo(data.unitVideo?.linkVideoURL)
        }
    }

    private fun selectedDot(position: Int) {
        for (i in 0 until carouselList.size) {
            if (i == position) {
                dots[i].setTextColor(ContextCompat.getColor(this, R.color.black))
            } else {
                dots[i].setTextColor(ContextCompat.getColor(this, R.color.grey_500))
            }
        }
    }

    private fun setIndicator() {
        for (i in 0 until carouselList.size) {
            dots.add(TextView(this))
            dots[i].text = Html.fromHtml("&#9679", Html.FROM_HTML_MODE_LEGACY).toString()
            dots[i].textSize = 18f
            binding.dotsIndicator.addView(dots[i])
        }
    }

    private fun loadImage(photos: List<UnitDetailResponse.Unit.UnitPhoto>?) {
        if (!photos.isNullOrEmpty()) {
            for (photo in photos) {
                Log.d("UnitDetailActivity", "photo: ${photo.filename}")

                val imageURL: String = DomainURL.DOMAIN + photo.filename
                Log.d("UnitDetailActivity", "imageURL: $imageURL")

                carouselList.add(
                    ImageData(imageURL)
                )
            }

        } else {
            Log.w("UnitDetailActivity", "photos is null")

            val drawableUri = Uri.parse("android.resource://$packageName/${R.drawable.placeholder}")
            val drawableUriString = drawableUri.toString()

            carouselList.add(
                ImageData(drawableUriString)
            )
        }
    }

    private fun loadVideo(videoUrl: String?) {
        Log.d("UnitDetailActivity", "loadVideo: $videoUrl")
        if (videoUrl != null) {
            Log.d("UnitDetailActivity", "video: ${videoUrl}")

            val webView: WebView = binding.webViewVideo

            val videoId = extractVideoIdFromUrl(videoUrl)
            if (videoId == "") {
                Log.w("UnitDetailActivity", "videoId is empty")
                return@loadVideo
            }
            webView.settings.javaScriptEnabled = true
            webView.loadUrl("https://www.youtube.com/embed/$videoId")

            binding.videoFrameContainer.visibility = TextView.VISIBLE
            binding.textViewVideoLabel.visibility = TextView.VISIBLE

        } else {
            Log.w("UnitDetailActivity", "videos is null")
            binding.videoFrameContainer.visibility = TextView.GONE
            binding.textViewVideoLabel.visibility = TextView.GONE
        }
    }

    private fun extractVideoIdFromUrl(url: String): String {
        val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
        val compiledPattern = Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(url)

        return if (matcher.find()) {
            matcher.group()
        } else {
            Log.w("UnitDetailActivity", "No video id found")
            ""
        }
    }

    private fun loadTextBased(data: UnitDetailResponse.Unit) {
        with(binding) {
            textViewUnitCode.text = data.unitCode
            textViewTitleUnitDetail.text = data.title
            textViewDescriptionUnit.text = data.description
            textViewPropertyType.text = data.propertyType

            textViewBuildingArea.text = NumericalUnitConverter.meterSquareFormatter(data.buildingArea ?: "0")
            textViewSurfaceArea.text = NumericalUnitConverter.meterSquareFormatter(data.surfaceArea ?: "0")
            textViewStaircase.text = data.floor
            textViewBed.text = data.bedroom
            textViewBath.text = data.bathroom
            textViewGarage.text = data.garage
            textViewPower.text = data.powerSupply
            textViewWater.text = data.waterSupply
            textViewInterior.text = data.interior
            textViewRoad.text = data.roadAccess

            textViewPriceUnit.text = NumericalUnitConverter.unitFormatter(data.price ?: "0", true)
        }
    }
}