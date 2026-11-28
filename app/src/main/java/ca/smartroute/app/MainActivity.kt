
package ca.smartroute.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 30, 24, 24)
            setBackgroundColor(Color.rgb(18, 24, 32))
        }

        fun addText(text: String, size: Float) {
            val view = TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(Color.WHITE)
                setPadding(0, 12, 0, 12)
            }
            root.addView(view)
        }

        addText("SmartRoute", 30f)
        addText("Your navigation assistant", 16f)

        val search = EditText(this).apply {
            hint = "Enter destination"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(16, 12, 16, 12)
        }
        root.addView(search)

        addText("MAP", 22f)
        addText("Your map will appear here", 16f)

        addText("CURRENT SPEED", 14f)
        addText("0 km/h", 32f)

        addText("SPEED LIMIT", 14f)
        addText("Unknown", 24f)

        val start = Button(this).apply {
            text = "START NAVIGATION"
            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Navigation feature is coming next",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        root.addView(start)

        val settings = Button(this).apply {
            text = "SETTINGS"
        }
        root.addView(settings)

        setContentView(root)
    }
}
