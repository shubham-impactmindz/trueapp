
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.data.dto.dashboard_banners.BigSliders
import com.app.truewebapp.databinding.ItemBannerBinding
import com.app.truewebapp.ui.component.main.dashboard.BigBannerListener
import com.bumptech.glide.Glide

class DashboardBannerAdapter(
    private val listener: BigBannerListener,
    private val banners: List<BigSliders>,
    private val cdnURL: String,
) : RecyclerView.Adapter<DashboardBannerAdapter.BannerViewHolder>() {


    inner class BannerViewHolder(private val binding: ItemBannerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(imageRes: String, position: Int) {
            Glide.with(binding.root.context)
                .load(cdnURL + imageRes)
                .thumbnail(0.1f)
                .dontAnimate()
                .into(binding.imageView)
            binding.imageView.setOnClickListener {
                listener.onUpdateBigBanner(banners[position].main_mcat_id.toString(), banners[position].mcat_id.toString(), banners[position].msubcat_id.toString(), banners[position].mproduct_id.toString())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(banners[position].home_large_banner_image, position)
    }

    override fun getItemCount(): Int = banners.size
}

