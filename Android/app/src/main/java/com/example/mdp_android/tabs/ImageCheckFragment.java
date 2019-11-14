package com.example.mdp_android.tabs;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.mdp_android.Constants;
import com.example.mdp_android.MainActivity;
import com.example.mdp_android.R;

import java.lang.reflect.Array;

public class ImageCheckFragment extends Fragment implements MainActivity.CallbackFragment
{
    private String[] _imageDataArr = new String[]{"0"};

    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.activity_imagecheck, container, false);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        updateImageFragment();
    }

    private void updateImageFragment() {

        TableLayout imageTable = (TableLayout) getView().findViewById(R.id.image_table);

        while (imageTable.getChildCount() > 1) {
            TableRow row =  (TableRow)imageTable.getChildAt(1);
            imageTable.removeView(row);
        }

        for (int i = 1; i < _imageDataArr.length; i += 4) {
            TableRow tbrow = new TableRow(getActivity());

            TextView t1v = new TextView(getActivity());
            t1v.setText(_imageDataArr[i]);
            t1v.setTextColor(Color.BLACK);
            t1v.setGravity(Gravity.CENTER);
            tbrow.addView(t1v);

            TextView t2v = new TextView(getActivity());
            t2v.setText(_imageDataArr[i + 1]);
            t2v.setTextColor(Color.BLACK);
            t2v.setGravity(Gravity.CENTER);
            tbrow.addView(t2v);

            TextView t3v = new TextView(getActivity());
            t3v.setText(_imageDataArr[i + 2]);
            t3v.setTextColor(Color.BLACK);
            t3v.setGravity(Gravity.CENTER);
            tbrow.addView(t3v);

            TextView t4v = new TextView(getActivity());
            t4v.setText(_imageDataArr[i + 3]);
            t4v.setTextColor(Color.BLACK);
            t4v.setGravity(Gravity.CENTER);
            tbrow.addView(t4v);

            imageTable.addView(tbrow);
        }

        if (_imageDataArr.length > 4) {
            String[][] imgStringArr = new String[(_imageDataArr.length - 1) / 4][3];
            int j = 1;
            for (int i = 1; i < (_imageDataArr.length - 1) / 4 + 1; i++) {
                imgStringArr[i - 1][0] = _imageDataArr[j];
                imgStringArr[i - 1][1] = _imageDataArr[j + 1];
                imgStringArr[i - 1][2] = _imageDataArr[j + 2];
                j += 4;
            }

            String imgString = "{ ";
            imgString += "(" + imgStringArr[0][0] + ", " + imgStringArr[0][1] + ", " + imgStringArr[0][2] + ")";
            for (int i = 1; i < (_imageDataArr.length - 1) / 4; i++) {
                imgString += ", (" + imgStringArr[i][0] + ", " + imgStringArr[i][1] + ", " + imgStringArr[i][2] + ")";
            }
            imgString += " }";

            TextView imgProcessString = getView().findViewById(R.id.ipsTextView);
            imgProcessString.setText(imgString);
        }
    }

    @Override
    public void update(int type, String key, String msg) {
        {
            if (key != null) {
                key = key.trim();
            }

            if (msg != null) {
                msg = msg.trim();
            }

            if (type == Constants.MESSAGE_READ && key.equals("IMAGE")) {
                String[] tmp = msg.split("-");
                String[] newImageEntry = MapFragment.getMaze().convertImgCoord(tmp).clone();
                processNewImageEntry(newImageEntry);
                onStart();
            }

            else if(type == Constants.MESSAGE_READ && key.equals("RESIMG")) {
                String[] unresolvedImageDataArr = MapFragment.getMaze().resolveMisplacedImages();
                if (unresolvedImageDataArr.length > 4) {
                    String[][] imgStringArr = new String[(unresolvedImageDataArr.length - 1) / 4][4];
                    int j = 1;
                    for (int i = 0; i < (unresolvedImageDataArr.length - 1) / 4; i++) {
                        imgStringArr[i][0] = unresolvedImageDataArr[j];
                        imgStringArr[i][1] = unresolvedImageDataArr[j + 1];
                        imgStringArr[i][2] = unresolvedImageDataArr[j + 2];
                        imgStringArr[i][3] = unresolvedImageDataArr[j + 3];

                        processNewImageEntry(MapFragment.getMaze().convertImgCoord(imgStringArr[i]).clone());

                        j += 4;
                    }
                    onStart();
                }
            }
        }
    }

    public void processNewImageEntry(String[] newImageEntry){
        if (_imageDataArr.length < 4){
            _imageDataArr = joinArray(_imageDataArr, newImageEntry);
        }
        if (_imageDataArr.length > 4){
            for(int j = 1; j < _imageDataArr.length ; j += 4){
                if (newImageEntry[0].equals(_imageDataArr[j])){
                    _imageDataArr[j] = newImageEntry[0];
                    _imageDataArr[j+1] = newImageEntry[2];
                    _imageDataArr[j+2] = newImageEntry[1];
                    _imageDataArr[j+3] = newImageEntry[3];
                    break;
                }
                if (j + 4 == _imageDataArr.length) {
                    _imageDataArr = joinArray(_imageDataArr, newImageEntry);
                }
            }
        }
    }

    public static <T> T[] joinArray(T[]... arrays) {
        int length = 0;
        for (T[] array : arrays) {
            length += array.length;
        }

        final T[] result = (T[]) Array.newInstance(arrays[0].getClass().getComponentType(), length);

        int offset = 0;
        for (T[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }

        return result;
    }

}