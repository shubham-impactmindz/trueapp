
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.truewebapp.data.dto.dashboard_banners.FruitsSliders
import com.app.truewebapp.databinding.ItemBannerDealBinding
import com.app.truewebapp.ui.component.main.dashboard.FruitsBannerListener
import com.bumptech.glide.Glide

class NonScrollingBannerFruitsAdapter(
    private val listener: FruitsBannerListener, private val banners: List<FruitsSliders>, private val  cdnURL: String) : RecyclerView.Adapter<NonScrollingBannerFruitsAdapter.BannerViewHolder>() {

    inner class BannerViewHolder(private val binding: ItemBannerDealBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(imageRes: String, position: Int) {
            Glide.with(binding.root.context)
                .load(cdnURL + imageRes)
                .thumbnail(0.1f)
                .dontAnimate()
                .into(binding.imageView)
            binding.imageView.setOnClickListener {
                listener.onUpdateFruitsBanner(banners[position].main_mcat_id.toString(), banners[position].mcat_id.toString(), banners[position].msubcat_id.toString(), banners[position].mproduct_id.toString())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemBannerDealBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(banners[position].home_fruit_banner_image, position)
    }

    override fun getItemCount(): Int = banners.size
}
