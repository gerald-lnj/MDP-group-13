package com.example.mdp_android.tabs;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mdp_android.MainActivity;
import com.example.mdp_android.R;

public class ImageCheckFragment extends Fragment implements MainActivity.CallbackFragment
{
    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.activity_imagecheck, container, false);
    }

    @Override
    public void update(int type, String key, String msg)
    {

    }
}
