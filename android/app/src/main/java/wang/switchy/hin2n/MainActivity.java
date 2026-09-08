package wang.switchy.hin2n;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.*;
import wang.switchy.hin2n.service.N2NService;

public class MainActivity extends AppCompatActivity {
    private Profile profile;
    private TextInputLayout server,community,key,ip,mask,mtu,mac;
    private MaterialAutoCompleteTextView cipher;
    private MaterialSwitch header;
    private MaterialButton connect,save;
    private TextView status,detail;
    private LinearLayout fields;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){public void run(){update();handler.postDelayed(this,700);}};
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout.LayoutParams spacing(int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(bottom);return p;}
    private TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);return t;}
    @Override public void onCreate(Bundle state){
        super.onCreate(state); profile=Profile.load(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout page=column();page.setPadding(dp(24),dp(24),dp(24),dp(24));scroll.addView(page);
        ViewCompat.setOnApplyWindowInsetsListener(scroll,(view,insets)->{
            Insets bar=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.ime());
            view.setPadding(bar.left,bar.top,bar.right,bar.bottom); return insets;
        });
        ImageView brandIcon=new ImageView(this);brandIcon.setImageResource(wang.switchy.hin2n.R.mipmap.ic_launcher);
        brandIcon.setContentDescription("n2n edge");page.addView(brandIcon,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView eyebrow=text("n2n edge",18);eyebrow.setLetterSpacing(0.04f);
        eyebrow.setTextColor(MaterialColors.getColor(this,androidx.appcompat.R.attr.colorPrimary,0));
        page.addView(eyebrow,spacing(8));
        page.addView(text("让设备，在同一子网",28),spacing(8));
        page.addView(text("通过你的节点，连接手机与其他设备。",14),spacing(24));
        MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(24));card.setStrokeWidth(0);
        card.setCardBackgroundColor(MaterialColors.getColor(this,com.google.android.material.R.attr.colorPrimaryContainer,0));
        LinearLayout inside=column();inside.setPadding(dp(24),dp(24),dp(24),dp(20));
        int foreground=MaterialColors.getColor(this,com.google.android.material.R.attr.colorOnPrimaryContainer,0);
        TextView label=text("连接状态",12);label.setTextColor(foreground);inside.addView(label,spacing(8));
        status=text("未连接",24);status.setTextColor(foreground);inside.addView(status,spacing(6));
        detail=text("",13);detail.setTextColor(foreground);inside.addView(detail,spacing(16));
        connect=new MaterialButton(this);connect.setMinHeight(dp(52));connect.setText("连接子网");
        inside.addView(connect,new LinearLayout.LayoutParams(-1,dp(60)));card.addView(inside);page.addView(card,spacing(24));
        fields=column();page.addView(fields,spacing(8));
        fields.addView(text("网络配置",20),spacing(16));
        server=input(fields,"节点地址",profile.server,false);server.setHelperText("Supernode · 主机名或 IPv4:端口");
        community=input(fields,"Community",profile.community,false);
        key=input(fields,"共享密钥",profile.key,true);key.setHelperText("同一社区的设备需使用相同密钥");
        ip=input(fields,"虚拟 IP",profile.ip,false);ip.setHelperText("每台设备使用唯一 IP");
        mask=input(fields,"子网掩码",profile.mask,false);
        MaterialButton advanced=new MaterialButton(this,null,com.google.android.material.R.attr.materialButtonOutlinedStyle);
        advanced.setText("高级设置 ▾");fields.addView(advanced,spacing(8));
        LinearLayout extras=column();extras.setVisibility(View.GONE);fields.addView(extras);
        advanced.setOnClickListener(v->{boolean show=extras.getVisibility()!=View.VISIBLE;extras.setVisibility(show?View.VISIBLE:View.GONE);advanced.setText(show?"收起高级设置 ▴":"高级设置 ▾");});
        TextInputLayout cipherBox=new TextInputLayout(this,null,com.google.android.material.R.attr.textInputOutlinedExposedDropdownMenuStyle);
        cipherBox.setHint("加密算法");cipher=new MaterialAutoCompleteTextView(cipherBox.getContext());cipher.setInputType(InputType.TYPE_NULL);
        cipher.setSimpleItems(new String[]{"AES-CBC","Twofish","Speck-CTR","ChaCha20"});cipher.setText(profile.cipher,false);
        cipherBox.addView(cipher);extras.addView(cipherBox,spacing(16));
        mtu=input(extras,"MTU",String.valueOf(profile.mtu),false);mtu.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        mac=input(extras,"MAC 地址",profile.mac,false);
        header=new MaterialSwitch(this);header.setText("加密协议头");header.setChecked(profile.header);extras.addView(header,spacing(16));
        extras.addView(text("协议：n2n v3 · UDP\n只路由上述 IPv4 子网。其他设备需使用一致的加密算法和协议头设置。启用协议头加密前，需由节点预设 Community。",13),spacing(16));
        save=new MaterialButton(this,null,com.google.android.material.R.attr.materialButtonOutlinedStyle);save.setText("保存配置");fields.addView(save,spacing(8));
        TextView note=text("免 root · 配置保存在此设备\n连接期间如需修改参数，请先断开。",12);page.addView(note,spacing(12));
        MaterialButton logs=new MaterialButton(this,null,com.google.android.material.R.attr.materialButtonOutlinedStyle);logs.setText("查看连接日志");page.addView(logs);
        logs.setOnClickListener(v->showLogs());
        save.setOnClickListener(v->{if(store()) Toast.makeText(this,"配置已保存",Toast.LENGTH_SHORT).show();});
        connect.setOnClickListener(v->{
            if(N2NService.active){startService(new Intent(this,N2NService.class).setAction("STOP"));return;}
            if(!store())return;
            Intent permission=VpnService.prepare(this);
            if(permission!=null)startActivityForResult(permission,10);else startVpn();
        });
        setContentView(scroll);
    }
    private TextInputLayout input(LinearLayout parent,String hint,String value,boolean secret){
        TextInputLayout box=new TextInputLayout(this,null,com.google.android.material.R.attr.textInputOutlinedStyle);box.setHint(hint);
        TextInputEditText edit=new TextInputEditText(box.getContext());edit.setSingleLine(true);
        edit.setInputType(InputType.TYPE_CLASS_TEXT|(secret?InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD));
        edit.setText(value);if(secret)edit.setSaveEnabled(false);box.addView(edit);
        if(secret)box.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        parent.addView(box,spacing(18));return box;
    }
    private String value(TextInputLayout box){return box.getEditText().getText().toString().trim();}
    private boolean store(){
        try {
            TextInputLayout[] boxes={server,community,key,ip,mask,mtu,mac};
            String[] labels={"节点地址","Community","共享密钥","虚拟 IP","子网掩码","MTU","MAC 地址"};
            java.util.ArrayList<String> missing=new java.util.ArrayList<>();
            for(int i=0;i<boxes.length;i++) {
                boxes[i].setError(null);
                String entered=boxes[i]==key?key.getEditText().getText().toString():value(boxes[i]);
                if(entered.isEmpty()) { missing.add(labels[i]);boxes[i].setError("请填写"+labels[i]); }
            }
            if(cipher.getText().toString().trim().isEmpty()) missing.add("加密算法");
            if(!missing.isEmpty()) throw new IllegalArgumentException("请填写："+String.join("、",missing));
            profile.server=value(server);profile.community=value(community);profile.key=key.getEditText().getText().toString();
            profile.ip=value(ip);profile.mask=value(mask);profile.mac=value(mac);profile.mtu=Integer.parseInt(value(mtu));
            profile.cipher=cipher.getText().toString();profile.header=header.isChecked();profile.save(this);return true;
        }catch(Exception e){new MaterialAlertDialogBuilder(this).setTitle("检查网络配置").setMessage(e instanceof NumberFormatException?"MTU 请输入数字":e.getMessage()).setPositiveButton("知道了",null).show();return false;}
    }
    private void startVpn(){
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);
        startForegroundService(new Intent(this,N2NService.class).setAction("START"));
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==10 && result==RESULT_OK)startVpn();}
    private void enable(View v,boolean enabled){v.setEnabled(enabled);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)enable(((ViewGroup)v).getChildAt(i),enabled);}
    private void update(){status.setText(N2NService.status);detail.setText(N2NService.detail);connect.setText(N2NService.active?"断开连接":"连接子网");enable(fields,!N2NService.active);}
    @Override protected void onResume(){super.onResume();handler.post(refresh);}
    @Override protected void onPause(){handler.removeCallbacks(refresh);super.onPause();}
    private void showLogs(){
        String result="尚无连接日志";
        try(java.io.RandomAccessFile f=new java.io.RandomAccessFile(new java.io.File(getFilesDir(),"edge.log"),"r")){
            long start=Math.max(0,f.length()-16000);f.seek(start);byte[] b=new byte[(int)(f.length()-start)];f.readFully(b);result=new String(b,java.nio.charset.StandardCharsets.UTF_8);
        }catch(Exception ignored){}
        TextView content=text(result,12);content.setTextIsSelectable(true);content.setPadding(dp(20),dp(16),dp(20),dp(16));
        ScrollView scroll=new ScrollView(this);scroll.addView(content);
        new MaterialAlertDialogBuilder(this).setTitle("连接日志").setView(scroll).setPositiveButton("关闭",null).show();
    }
}
