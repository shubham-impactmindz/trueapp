
import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.R
import com.app.truewebapp.databinding.ItemDashboardProductsBinding
import com.app.truewebapp.ui.component.main.shop.Product
import com.app.truewebapp.utils.GlideApp

class NonScrollingBannerDrinksAdapter(private val products: List<Product>?,private val title: String?) : RecyclerView.Adapter<NonScrollingBannerDrinksAdapter.BannerViewHolder>() {

    inner class BannerViewHolder(val binding: ItemDashboardProductsBinding) : RecyclerView.ViewHolder(binding.root) {
        private var count = 0 // Maintain count state per item

        fun bind(option: Product?, position: Int) {
            binding.textBrand.text = title ?: "Unknown Brand"
            binding.textTitle.text = option?.title ?: "No Title"

            // Load image using Glide
            GlideApp.with(binding.imgProduct.context)
                .load(option?.img)
                .placeholder(R.drawable.ic_lays)
                .error(R.drawable.ic_lays)
                .into(binding.imgProduct)

            // Strike-through effect for compare price
            binding.comparePrice.paintFlags = Paint.STRIKE_THRU_TEXT_FLAG

            // Button click listeners
            binding.btnAdd.setOnClickListener {
                count = 1
                binding.textNoOfItems.text = count.toString()
                binding.btnAdd.visibility = View.GONE
                binding.llCartSign.visibility = View.VISIBLE
            }

            binding.btnAddMore.setOnClickListener {
                count++
                binding.textNoOfItems.text = count.toString()
            }

            binding.btnMinus.setOnClickListener {
                if (count > 1) {
                    count--
                    binding.textNoOfItems.text = count.toString()
                } else {
                    count = 0
                    binding.llCartSign.visibility = View.GONE
                    binding.btnAdd.visibility = View.VISIBLE
                }
            }

            if (position == 1 || position == 3) {
                binding.lottieCheckmark.setAnimation(R.raw.sale) // R.raw.sale -> sale.json
            } else {
                binding.lottieCheckmark.setAnimation(R.raw.flash_deals) // R.raw.black_friday -> black_friday.json
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemDashboardProductsBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        // Adjust item width dynamically based on screen size
//        val displayMetrics = parent.context.resources.displayMetrics
//        val screenWidth = displayMetrics.widthPixels
//        val itemWidth = (screenWidth / 2.2).toInt() // Adjusting width dynamically
//
//        // Apply the calculated width to the root view of the binding
//        binding.root.layoutParams = ViewGroup.LayoutParams(itemWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

        return BannerViewHolder(binding)
    }


    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val option = products?.get(position)
        Log.d("setupShopUI", "Products Loaded: $option")
        holder.bind(option, position)
    }

    override fun getItemCount(): Int = products?.size ?: 0
}
