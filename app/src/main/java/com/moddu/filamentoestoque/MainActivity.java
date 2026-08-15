package com.moddu.filamentoestoque;

import android.app.*;
import android.os.*;
import android.provider.MediaStore;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import androidx.core.content.FileProvider;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
    private DbHelper db;
    private LinearLayout list, stats;
    private EditText search;
    private String filter = "TODOS";
    private final int REQ_PHOTO = 10, REQ_QR = 11, REQ_CAMERA = 20;
    private Uri cameraUri;
    private File cameraTempFile;
    private boolean cameraCaptureInProgress;
    private boolean cameraForQr;
    private ImageView pendingImage;
    private Filament editing;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b); db = new DbHelper(this); buildHome();
    }

    private TextView tv(String s, int sp, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(Color.rgb(31,41,55));
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD); return t;
    }
    private int dp(int n){ return (int)(n * getResources().getDisplayMetrics().density); }
    private GradientDrawable bg(int color, int radius) { GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    private void pad(View v,int n){v.setPadding(dp(n),dp(n),dp(n),dp(n));}

    private void buildHome() {
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(246,247,251));
        TextView head=tv("Estoque de Filamentos",24,true); head.setTextColor(Color.WHITE); head.setBackgroundColor(Color.rgb(17,24,39)); pad(head,18); root.addView(head);

        stats=new LinearLayout(this); stats.setOrientation(LinearLayout.HORIZONTAL); stats.setPadding(dp(12),dp(12),dp(12),dp(6)); root.addView(stats); refreshStats();

        search=new EditText(this); search.setHint("Pesquisar cor, marca ou especificação..."); search.setSingleLine(true); search.setBackground(bg(Color.WHITE,12)); pad(search,12);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(52)); sp.setMargins(dp(12),dp(4),dp(12),dp(8)); root.addView(search,sp);
        search.addTextChangedListener(new android.text.TextWatcher(){ public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){refreshList();} public void afterTextChanged(android.text.Editable e){} });

        LinearLayout chips=new LinearLayout(this); chips.setOrientation(LinearLayout.HORIZONTAL); chips.setPadding(dp(12),0,dp(12),dp(8));
        for(String m:new String[]{"TODOS","PLA","PETG","TPU"}){ Button bt=new Button(this); bt.setText(m); bt.setTextSize(12); bt.setAllCaps(false); bt.setOnClickListener(v->{filter=((Button)v).getText().toString();refreshList();}); chips.addView(bt,new LinearLayout.LayoutParams(0,dp(44),1)); }
        root.addView(chips);

        ScrollView sv=new ScrollView(this); list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(12),0,dp(12),dp(80)); sv.addView(list); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));

        Button add=new Button(this); add.setText("+ Cadastrar filamento"); add.setTextSize(17); add.setTextColor(Color.WHITE); add.setBackground(bg(Color.rgb(91,91,214),16)); add.setOnClickListener(v->showForm(new Filament()));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(58)); ap.setMargins(dp(12),dp(8),dp(12),dp(14)); root.addView(add,ap);
        setContentView(root); refreshList();
    }

    private void refreshStats(){
        if(stats==null)return; stats.removeAllViews();
        for(String m:new String[]{"PLA","PETG","TPU"}){ TextView x=tv(m+"\n"+db.count(m)+" rolos",15,true); x.setGravity(Gravity.CENTER); x.setBackground(bg(Color.WHITE,12)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(72),1); p.setMargins(dp(3),0,dp(3),0); stats.addView(x,p); }
    }

    private void refreshList(){
        if(list==null)return; list.removeAllViews(); String q=search==null?"":search.getText().toString(); List<Filament> data=db.list(filter,q);
        if(data.isEmpty()){TextView e=tv("Nenhum filamento cadastrado nesta categoria.",16,false); e.setGravity(Gravity.CENTER); e.setPadding(0,dp(50),0,0); list.addView(e);return;}
        for(Filament f:data) list.addView(card(f));
    }

    private View card(Filament f){
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.HORIZONTAL); c.setBackground(bg(Color.WHITE,14)); pad(c,10);
        ImageView im=new ImageView(this); im.setScaleType(ImageView.ScaleType.CENTER_CROP); im.setImageResource(android.R.drawable.ic_menu_gallery); loadImage(im,f.photoPath); c.addView(im,new LinearLayout.LayoutParams(dp(84),dp(84)));
        LinearLayout txt=new LinearLayout(this); txt.setOrientation(LinearLayout.VERTICAL); txt.setPadding(dp(12),0,0,0);
        TextView title=tv(f.material+" • "+empty(f.color,"Sem cor"),17,true); txt.addView(title);
        txt.addView(tv(empty(f.brand,"Marca não informada"),14,false));
        int pct=f.initialWeight>0?(int)(100.0*f.remainingWeight/f.initialWeight):0; pct=Math.max(0,Math.min(100,pct)); TextView w=tv("Restante: "+f.remainingWeight+" g ("+pct+"%)",14,pct<=20); if(pct<=20)w.setTextColor(Color.rgb(190,45,45)); txt.addView(w);
        if(!f.qrInfo.trim().isEmpty()) txt.addView(tv("QR: "+f.qrInfo,12,false));
        c.addView(txt,new LinearLayout.LayoutParams(0,-2,1));
        c.setOnClickListener(v->showForm(f));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,0,0,dp(10)); c.setLayoutParams(p); return c;
    }

    private String empty(String s,String d){ return s==null||s.trim().isEmpty()?d:s; }

    private void showForm(Filament f){
        editing=f; final Dialog d=new Dialog(this); d.setTitle(f.id>0?"Editar filamento":"Novo filamento");
        ScrollView sv=new ScrollView(this); LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18),dp(12),dp(18),dp(20)); sv.addView(box);
        TextView h=tv(f.id>0?"Editar filamento":"Cadastrar filamento",22,true); box.addView(h);
        Spinner mat=new Spinner(this); mat.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"PLA","PETG","TPU"})); mat.setSelection(Math.max(0,Arrays.asList("PLA","PETG","TPU").indexOf(f.material))); box.addView(label("Material",mat));
        EditText color=field("Cor",f.color); box.addView(color); EditText brand=field("Marca / fabricante",f.brand); box.addView(brand);
        EditText initial=field("Peso inicial (g)",String.valueOf(f.initialWeight)); initial.setInputType(2); box.addView(initial);
        EditText remaining=field("Peso restante (g)",String.valueOf(f.remainingWeight)); remaining.setInputType(2); box.addView(remaining);
        EditText qrInfo=field("Especificações / conteúdo do QR Code",f.qrInfo); box.addView(qrInfo);
        EditText notes=field("Observações (temperatura, lote, etc.)",f.notes); notes.setMinLines(2); box.addView(notes);

        ImageView photo=new ImageView(this); photo.setScaleType(ImageView.ScaleType.CENTER_CROP); photo.setImageResource(android.R.drawable.ic_menu_camera); loadImage(photo,f.photoPath); box.addView(photo,new LinearLayout.LayoutParams(-1,dp(180)));
        LinearLayout pb=new LinearLayout(this); Button cam=new Button(this); cam.setText("Tirar foto"); Button gal=new Button(this); gal.setText("Galeria"); pb.addView(cam,new LinearLayout.LayoutParams(0,dp(48),1)); pb.addView(gal,new LinearLayout.LayoutParams(0,dp(48),1)); box.addView(pb);
        cam.setOnClickListener(v->{pendingImage=photo; cameraForQr=false; takeCamera();}); gal.setOnClickListener(v->{pendingImage=photo; cameraForQr=false; pickImage(REQ_PHOTO);});

        TextView qlab=tv("Foto do QR Code",16,true); qlab.setPadding(0,dp(14),0,dp(6)); box.addView(qlab);
        ImageView qr=new ImageView(this); qr.setScaleType(ImageView.ScaleType.CENTER_CROP); qr.setImageResource(android.R.drawable.ic_menu_camera); loadImage(qr,f.qrPhotoPath); box.addView(qr,new LinearLayout.LayoutParams(-1,dp(160)));
        LinearLayout qb=new LinearLayout(this); Button qcam=new Button(this); qcam.setText("Fotografar QR"); Button qgal=new Button(this); qgal.setText("Galeria"); qb.addView(qcam,new LinearLayout.LayoutParams(0,dp(48),1)); qb.addView(qgal,new LinearLayout.LayoutParams(0,dp(48),1)); box.addView(qb);
        qcam.setOnClickListener(v->{pendingImage=qr;cameraForQr=true;takeCamera();}); qgal.setOnClickListener(v->{pendingImage=qr;cameraForQr=true;pickImage(REQ_QR);});

        LinearLayout actions=new LinearLayout(this); Button save=new Button(this); save.setText("SALVAR"); actions.addView(save,new LinearLayout.LayoutParams(0,dp(54),2));
        if(f.id>0){Button del=new Button(this);del.setText("EXCLUIR");actions.addView(del,new LinearLayout.LayoutParams(0,dp(54),1));del.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Excluir este filamento?").setNegativeButton("Cancelar",null).setPositiveButton("Excluir",(x,y)->{db.delete(f.id);d.dismiss();refreshStats();refreshList();}).show());}
        box.addView(actions);
        save.setOnClickListener(v->{
            int initialValue=num(initial,-1); int remainingValue=num(remaining,-1);
            if(initialValue<=0){initial.setError("Informe um peso inicial maior que zero");initial.requestFocus();return;}
            if(remainingValue<0){remaining.setError("Informe um peso restante válido");remaining.requestFocus();return;}
            if(remainingValue>initialValue){remaining.setError("O peso restante não pode ser maior que o peso inicial");remaining.requestFocus();return;}
            f.material=mat.getSelectedItem().toString(); f.color=color.getText().toString().trim(); f.brand=brand.getText().toString().trim(); f.initialWeight=initialValue; f.remainingWeight=remainingValue; f.qrInfo=qrInfo.getText().toString().trim(); f.notes=notes.getText().toString().trim(); db.save(f); d.dismiss(); refreshStats(); refreshList(); Toast.makeText(this,"Filamento salvo",Toast.LENGTH_SHORT).show(); });
        d.setContentView(sv); Window w=d.getWindow(); if(w!=null)w.setLayout(-1,-1); d.show(); if(d.getWindow()!=null)d.getWindow().setLayout(-1,-1);
    }

    private LinearLayout label(String s,View v){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);TextView l=tv(s,13,true);b.addView(l);b.addView(v,new LinearLayout.LayoutParams(-1,dp(52)));return b;}
    private EditText field(String hint,String val){EditText e=new EditText(this);e.setHint(hint);e.setText(val==null?"":val);e.setSingleLine(false);e.setPadding(dp(10),dp(8),dp(10),dp(8));return e;}
    private int num(EditText e,int d){try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception x){return d;}}

    private void pickImage(int req){
        cleanupCameraTemp(); cameraCaptureInProgress=false;
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,req);
    }
    private void takeCamera(){
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(android.Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.CAMERA},REQ_CAMERA);return;}
        try{
            File dir=new File(getCacheDir(),"camera"); if(!dir.exists()&&!dir.mkdirs())throw new IOException("Falha ao criar pasta temporária");
            cameraTempFile=new File(dir,"filamento_"+System.currentTimeMillis()+".jpg");
            cameraUri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",cameraTempFile);
            cameraCaptureInProgress=true;
            Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE); i.putExtra(MediaStore.EXTRA_OUTPUT,cameraUri); i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if(i.resolveActivity(getPackageManager())==null){Toast.makeText(this,"Nenhum aplicativo de câmera disponível",Toast.LENGTH_LONG).show();cleanupCameraTemp();return;}
            startActivityForResult(i,cameraForQr?REQ_QR:REQ_PHOTO);
        }catch(Exception e){cleanupCameraTemp();Toast.makeText(this,"Não foi possível abrir a câmera",Toast.LENGTH_LONG).show();}
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grants){super.onRequestPermissionsResult(requestCode,permissions,grants);if(requestCode==REQ_CAMERA&&grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)takeCamera();else if(requestCode==REQ_CAMERA)Toast.makeText(this,"A permissão da câmera é necessária para tirar fotos",Toast.LENGTH_LONG).show();}
    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(result!=RESULT_OK){if(cameraCaptureInProgress)cleanupCameraTemp();return;}
        Uri u=cameraCaptureInProgress?cameraUri:(data==null?null:data.getData()); if(u==null){cleanupCameraTemp();return;}
        String path=copyToInternal(u,req==REQ_QR?"qr":"photo");
        if(!path.isEmpty()){if(req==REQ_QR)editing.qrPhotoPath=path; else editing.photoPath=path; if(pendingImage!=null)loadImage(pendingImage,path);}
        if(cameraCaptureInProgress)cleanupCameraTemp();
    }
    private void cleanupCameraTemp(){
        cameraCaptureInProgress=false; cameraUri=null;
        if(cameraTempFile!=null&&cameraTempFile.exists()){try{cameraTempFile.delete();}catch(Exception ignored){}}
        cameraTempFile=null;
    }
    private String copyToInternal(Uri u,String prefix){try{File out=new File(getFilesDir(),prefix+"_"+System.currentTimeMillis()+".jpg");InputStream in=getContentResolver().openInputStream(u);FileOutputStream fo=new FileOutputStream(out);byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)fo.write(buf,0,n);in.close();fo.close();return out.getAbsolutePath();}catch(Exception e){Toast.makeText(this,"Não foi possível salvar a imagem",Toast.LENGTH_LONG).show();return "";}}
    private void loadImage(ImageView im,String path){try{if(path!=null&&!path.isEmpty()&&new File(path).exists())im.setImageURI(Uri.fromFile(new File(path)));}catch(Exception ignored){}}
}
