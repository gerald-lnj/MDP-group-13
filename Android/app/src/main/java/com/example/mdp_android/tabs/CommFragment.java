package com.example.mdp_android.tabs;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.mdp_android.MainActivity;
import com.example.mdp_android.R;
import com.example.mdp_android.bluetooth.BluetoothChatService;
import com.example.mdp_android.bluetooth.BluetoothManager;

public class CommFragment extends Fragment implements MainActivity.CallbackFragment
{
    private static final String TAG = "CommFragment";

    private EditText msgOut;
    private EditText fn1String, fn2String;
    private Button sendBtn, updateBtn, fn1Btn, fn2Btn;
    private Button cancelBtn, saveBtn;
    private View popupInputDialogView;

    private BluetoothChatService mChatService = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private SharedPreferences mPreference;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.activity_communication, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        TextView msgReceived = getView().findViewById(R.id.msgReceived);
        msgReceived.setMovementMethod(new ScrollingMovementMethod());

        msgOut = view.findViewById(R.id.editText);
        sendBtn = view.findViewById(R.id.sendMsgBtn);
        updateBtn = view.findViewById(R.id.msgUpdateBtn);
        fn1Btn = view.findViewById(R.id.fn1Btn);
        fn2Btn = view.findViewById(R.id.fn2Btn);
        popupInputDialogView = getLayoutInflater().inflate(R.layout.activity_communication, null);

        mPreference = PreferenceManager.getDefaultSharedPreferences(getContext());
        mPreference = getActivity().getApplicationContext().getSharedPreferences("settings", 0);

        if (!(view instanceof EditText))
        {
            view.setOnTouchListener(new View.OnTouchListener()
            {
                public boolean onTouch(View v, MotionEvent event)
                {
                    return false;
                }
            });
        }

        updateBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
                alertDialogBuilder.setCancelable(true);

                initPopupViewControls();

                alertDialogBuilder.setView(popupInputDialogView);

                final AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();

                cancelBtn.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        alertDialog.dismiss();
                    }
                });

                saveBtn.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        SharedPreferences prefs = getActivity().getApplicationContext().getSharedPreferences("settings", 0);
                        SharedPreferences.Editor prefEdit = prefs.edit();
                        prefEdit.putString("s1", fn1String.getText().toString());
                        prefEdit.putString("s2", fn2String.getText().toString());
                        prefEdit.commit();
                        alertDialog.dismiss();
                    }
                });
            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                View view = getView();

                if (view != null)
                {
                    String message = msgOut.getText().toString();

                    if (BluetoothManager.getInstance().bluetoothAvailable() && BluetoothManager.getInstance().isConnected())
                    {
                        BluetoothManager.getInstance().sendMessage("F0", message);
                        MainActivity.updateMsgHistory("[ Sent: "+message+" ]");
                    }
                    else
                    {
                        BluetoothManager.getInstance().notConnectedMsg();
                    }
                }

                displayRecMsg();
            }
        });

        fn1Btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String s1 = mPreference.getString("s1", null);

                if (BluetoothManager.getInstance().bluetoothAvailable() && BluetoothManager.getInstance().isConnected() && s1 != null)
                {
                    BluetoothManager.getInstance().sendMessage("F1", s1);
                    MainActivity.updateMsgHistory("[ Sent (F1): "+s1+" ]");
                }
                else
                {
                    BluetoothManager.getInstance().notConnectedMsg();
                }

                displayRecMsg();
            }
        });

        fn2Btn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String s2 = mPreference.getString("s2", null);
                if (BluetoothManager.getInstance().bluetoothAvailable() && BluetoothManager.getInstance().isConnected() && s2 != null)
                {
                    BluetoothManager.getInstance().sendMessage("F2", s2);
                    MainActivity.updateMsgHistory("[ Sent (F2): "+s2+" ]");
                }
                else
                {
                    BluetoothManager.getInstance().notConnectedMsg();
                }

                displayRecMsg();
            }
        });

        displayRecMsg();
    }

    public void update(int type, String key, String msg)
    {
        if(key != null && key.equals("REC"))
        {
            MainActivity.updateMsgHistory("Received: "+msg);
        }

        displayRecMsg();
    }

    private void displayRecMsg()
    {
        if(getView() != null)
        {
            TextView msgIn = getView().findViewById(R.id.msgReceived);

            if(msgIn != null)
            {
                String text = "";
                for (String a : MainActivity.getMsgHistory()) text += a + "\n";
                msgIn.setText(text);
            }
        }
    }

    private void initPopupViewControls()
    {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());

        popupInputDialogView = layoutInflater.inflate(R.layout.activity_popupdialog, null);

        fn1String = popupInputDialogView.findViewById(R.id.fn1Edit);
        fn2String = popupInputDialogView.findViewById(R.id.fn2Edit);
        saveBtn = popupInputDialogView.findViewById(R.id.saveBtn);
        cancelBtn = popupInputDialogView.findViewById(R.id.cancelBtn);

        fn1String.setEnabled(true);
        fn2String.setEnabled(true);
        saveBtn.setEnabled(true);
        cancelBtn.setEnabled(true);

        fn1String.setText(mPreference.getString("s1", null));
        fn2String.setText(mPreference.getString("s2", null));
    }
}