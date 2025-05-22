
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.data.dto.browse.BrowseBanners
import com.app.truewebapp.databinding.ItemBannerBinding
import com.bumptech.glide.Glide

class BannerAdapter(
    private val banners: List<BrowseBanners>,
    private val cdnURL: String,
    private val listener: OnBannerClickListener
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    interface OnBannerClickListener {
        fun onBannerClick(mainCatid: String,catid: String, subcatid: String, productid: String)
    }

    inner class BannerViewHolder(private val binding: ItemBannerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(banner: BrowseBanners) {
            Glide.with(binding.root.context)
                .load(cdnURL + banner.browsebanner_image)
                .thumbnail(0.1f)
                .dontAnimate()
                .into(binding.imageView)

            binding.imageView.setOnClickListener {
                listener.onBannerClick(banner.main_mcat_id.toString(),banner.mcat_id.toString(), banner.msubcat_id.toString(), banner.mproduct_id.toString())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(banners[position])
    }

    override fun getItemCount(): Int = banners.size
}

