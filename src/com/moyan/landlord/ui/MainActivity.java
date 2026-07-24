
package com.moyan.landlord.ui;

import android.app.Activity;

import android.os.Bundle;

public class MainActivity extends Activity {

@Override

protected void onCreate(Bundle savedInstanceState) {

super.onCreate(savedInstanceState);

// 确保 res/layout/activity_main.xml 存在

setContentView(R.layout.activity_main);

System.out.println("Moyan Landlord App Started!");

}

}

