Android Split Pane Layout
===========

An Android layout which splits the available space between two child views. An optionally movable bar exists between the children which allows the user to redistribute the space allocated to each view.

Usage
=====
Add a reference to the split-pane-layout library project, or copy the necessary files into your project. Add a SplitPaneLayout as follows:

    <com.mobidevelop.spl.widget.SplitPaneLayout
        xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:spl="http://schemas.android.com/apk/res-auto"
        android:layout_width="fill_parent"
        android:layout_height="fill_parent"
        spl:orientation="vertical"
        spl:splitterSize="12dip"
        spl:splitterPosition="33%"
        spl:splitterBackground="@drawable/splitter_bg_v"
        >
        <TextView
            android:layout_width="fill_parent"
            android:layout_height="wrap_content"
            android:text="Child1" />
        <TextView
            android:layout_width="fill_parent"
            android:layout_height="wrap_content"
            android:text="Child2" />
    </com.mobidevelop.spl.widget.SplitPaneLayout>

**NOTE**: A SplitPaneLayout **MUST** have exactly two children.  

Using with Gradle
====================
SplitPaneLayout is published to Maven Central so can be easily added to your Gradle-based Android projects by adding the following entry to your dependencies:

```groovy
dependencies {
    compile 'com.mobidevelop.spl:split-pane-layout:1.0.0'
}
```


Precompiled Demo APK
====================
<https://www.box.com/s/6yw9kekmq558wgbwan91>

![QR](https://chart.googleapis.com/chart?cht=qr&chs=300x300&chl=https://www.box.com/s/6yw9kekmq558wgbwan91)
