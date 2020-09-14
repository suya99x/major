package com.example.trafficcongestion

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.trafficcongestion.activities.LoginActivity
import com.example.trafficcongestion.activities.UserDetails
import com.example.trafficcongestion.activities.Maps
import com.example.trafficcongestion.activities.Maps1
import com.example.trafficcongestion.modal.Predicated
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener{

    private var selectedday: EditText? = null
    private var btndata: Button? = null

    private val mKeys: ArrayList<String> = ArrayList()
    private val mData: ArrayList<Predicated> = ArrayList()
    private val entries :ArrayList<Entry> = ArrayList()

    private lateinit var dataref: DatabaseReference
    val ref = FirebaseDatabase.getInstance().getReference()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        btndata = findViewById<View>(R.id.data_button) as Button
        selectedday = findViewById<View>(R.id.selectday) as EditText
        val dayarry = resources.getStringArray(R.array.days_array)
        val drawlinechart=findViewById<View>(R.id.lineChart) as LineChart

        val spinner=findViewById<View>(R.id.days_spinner) as Spinner

        btndata!!.setOnClickListener {
            dataref = ref.child("Gwarko").child(selectedday?.text.toString())
            dataref.addValueEventListener(object : ValueEventListener {

                override fun onDataChange(p0: DataSnapshot) {
                    // Log.i("key1 ", "is" + p0.key)
                    //Log.i("value ", "is" + p0.getValue())
                   // val lineDataSet = LineDataSet(entries, "Temp")
                    p0.children.forEach {
                        //Log.i("key2 ", "is" + it.key)
                        //Log.i("value ", "is" + it.getValue())
                        val key = it.key
                        mKeys.add(key!!)
                        //Log.e("keys","are"+it.key)
                        //System.out.println(mKeys)
                        val retrive = it.getValue(Predicated::class.java)
                       // Log.e("retrived data", "are" + retrive?.Time + retrive?.Count)
                        mData.add(retrive!!)
                        //System.out.println(mData)

                        var count = retrive.Count!!.toFloat()
                        var time = retrive.Time!!.toFloat()

                        //for line chart
                        entries.add(Entry(time, count))
                        System.out.println(entries)
                    }
                    //System.out.println(list)
                    //val data = LineData(lineDataSet)
                    //drawlinechart.setData(data)
                    //drawlinechart.notifyDataSetChanged()
                   // drawlinechart.invalidate()
                    val dataSet = LineDataSet(entries,"plotted values")

                    //dataSet.setDrawValues(false)
                    //dataSet.setDrawFilled(false)(color fill)

                    //data to draw
                    drawlinechart.data = LineData(dataSet)
                    //auto update linechart
                    drawlinechart.notifyDataSetChanged()
                    drawlinechart.invalidate()
                    lineChart.setPinchZoom(true)
                    //to remove right y-axis
                    lineChart.axisRight.isEnabled = false
                    lineChart.description.text = "Time"
                    //set x-axis

                    lineChart.xAxis.position= XAxis.XAxisPosition.BOTTOM
                    lineChart.xAxis.granularity=1f



                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })


        }

        if (spinner != null) {
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item, dayarry
            )
            spinner.adapter = adapter
            spinner.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View, position: Int, id: Long
                ) {
                    selectedday!!.setText(dayarry[position]).toString()
                    mData.clear()
                    entries.clear()
                    Log.e("clearing", "plz...wait" + mData)
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.selected_item) + " " +
                                "" + dayarry[position], Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // write code to perform some action
                }
            }
        }

            val toggle = ActionBarDrawerToggle(
                this,
                drawer_layout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
            )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
    }

    override fun onBackPressed() {
    if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
        drawer_layout.closeDrawer(GravityCompat.START)
    } else {
        super.onBackPressed()
    }
}
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.user_login -> {
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    Log.v("Session", "Signed In")
                    startActivity(Intent(this, UserDetails::class.java))

                } else {
                    val changePage = Intent(this, LoginActivity::class.java)
                    startActivity(changePage)
                    Toast.makeText(this@MainActivity, "login/sign up", Toast.LENGTH_SHORT).show()
                }
            }

            R.id.navigation_saved -> {
                startActivity(Intent(this, Maps::class.java))
                drawer_layout.closeDrawers()
            }
            R.id.navigation_setting -> {
                startActivity(Intent(this, Maps1::class.java))
                drawer_layout.closeDrawers()
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

}











