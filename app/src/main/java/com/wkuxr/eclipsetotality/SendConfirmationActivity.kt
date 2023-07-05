package com.wkuxr.eclipsetotality

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wkuxr.eclipsetotality.databinding.ActivitySendConfirmationBinding
import com.wkuxr.eclipsetotality.database.Metadata
import com.wkuxr.eclipsetotality.database.MetadataDB.Companion.db
import java.io.File

class SendConfirmationActivity : AppCompatActivity() {
    lateinit var binding: ActivitySendConfirmationBinding
    lateinit var recyclerView: RecyclerView

    companion object {
        lateinit var prefs: SharedPreferences
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView
    }

    fun displayImageList(){
        val metadata = db.getMetadata()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ItemAdapter(metadata, supportFragmentManager, filesDir.absolutePath)
    }

    class ItemAdapter(private val metadataList: List<Metadata>, fragManager: FragmentManager, directory: String) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
        val fMan = fragManager
        val fDir = directory

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemAdapter.ItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.recycle_list_item, parent, false)
            return ItemViewHolder(view)
        }

        override fun getItemCount(): Int {
            return metadataList.size
        }

        override fun onBindViewHolder(holder: ItemAdapter.ItemViewHolder, position: Int) {
            val item = metadataList[position]
            holder.itemView.tag = item.id
            val imgFile = File(prefs.getString("imageFolderDirectory", fDir) + item.filepath)

            //create bitmap from image
            val imgBitmap = BitmapFactory.decodeFile(imgFile.absolutePath)

            holder.imgView.setImageBitmap(imgBitmap)
        }

        inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imgView: ImageView = itemView.findViewById(R.id.imageView)
        }
    }
}