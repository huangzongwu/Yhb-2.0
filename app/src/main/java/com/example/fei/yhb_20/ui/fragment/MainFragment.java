package com.example.fei.yhb_20.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bmob.BmobProFile;
import com.bmob.btp.callback.ThumbnailListener;
import com.example.fei.yhb_20.R;
import com.example.fei.yhb_20.bean.Post;
import com.example.fei.yhb_20.ui.DeatilActivity;
import com.example.fei.yhb_20.ui.GalleryUrlActivity;
import com.example.fei.yhb_20.ui.PersonalActivity;
import com.example.fei.yhb_20.utils.ACache;
import com.example.fei.yhb_20.utils.ExpressionUtil;
import com.example.fei.yhb_20.utils.MyUtils;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.UpdateListener;


public class MainFragment extends Fragment {
    private static final String TAG = "MainFragment";

    private RecyclerView recyclerView;
    private static Picasso picasso;
    private static DrawerLayout drawerLayout;
    private static ACache aCache;
    LinearLayoutManager layoutManager;
    private SharedPreferences sharedPreferences ;
    private static LinearLayout llFoot;
    private static Button send;
    private static EditText comment;
    private static ImageView face;
    private static Post currentPost;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.main_recyclerView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        send = (Button) view.findViewById(R.id.team_singlechat_id_send);
        comment = (EditText) view.findViewById(R.id.team_singlechat_id_edit);
        face = (ImageView) view.findViewById(R.id.team_singlechat_id_expression);
        llFoot = (LinearLayout) view.findViewById(R.id.team_singlechat_id_foot);

        face.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyUtils.showFaceDialog(getActivity(),comment);
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm= (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                llFoot.setVisibility(View.INVISIBLE);
                MyUtils.commentSend(currentPost,comment,getActivity());
                //强制隐藏
                imm.hideSoftInputFromWindow(comment.getWindowToken(),0);
                comment.setText(null);
            }
        });

        drawerLayout = (DrawerLayout) view.findViewById(R.id.drawer_layout);

        picasso = Picasso.with(getActivity());
        //TODO 不要在这里联网查询
//        recyclerView.setAdapter(new MyAdapter(data));
        return view;

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //TODO 在这里联网
        sharedPreferences = getActivity().getSharedPreferences("settings",0);
        BmobQuery<Post> query = new BmobQuery<Post>();
        query.include("user");
        query.order("-createdAt");
        query.findObjects(getActivity(),new FindListener<Post>() {
            @Override
            public void onSuccess(List<Post> posts) {
                //在这里写入缓存
                for (int i = 0 ;i<posts.size();i++){
                    aCache.put(String.valueOf(i),posts.get(i));
                    if (aCache.getAsBinary(posts.get(i).getObjectId()+"footerBoolean")==null){
                        byte[] footerBoolean = {1,1,1,1};
                        aCache.put(posts.get(i).getObjectId()+"footerBoolean",footerBoolean);
                    }
                    //根据ObjectId来
                    Log.e(TAG, "write success " + i);
                }
                aCache.put("cacheSize",String.valueOf(posts.size()));
                recyclerView.setAdapter(new MyAdapter(posts,getActivity(),drawerLayout));
            }

            @Override
            public void onError(int code, String s) {
                Log.e(TAG,s);
                if (sharedPreferences.getBoolean("ever",false)){
                    List<Post> objects = new ArrayList<Post>();

                    int size = Integer.parseInt(aCache.getAsString("cacheSize"));
                    Post post;
                    for (int i = 0;i<size;i++){
                        post= (Post) aCache.getAsObject(String.valueOf(i));
                        if (post==null){
                            android.util.Log.e(TAG, "post is null");
                        }
                        objects.add(post);
                    }
                    recyclerView.setAdapter(new MyAdapter(objects,getActivity(),drawerLayout));
                }else{
                    Toast.makeText(getActivity(),"您没有登录过，没有缓存文件！",Toast.LENGTH_LONG).show();
                }

            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        aCache = ACache.get(getActivity());
    }

    private static class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private static final int SHARE = 0;
        private static final int LIKE = 1;
        private static final int DISLIKE = 2;
        private static final int COMMENT = 3;
        private int lastPosition = -1;
        private List<Post> data;
        private Context context;

        public MyAdapter(List<Post> data,Context context,View view) {
            this.data = data;
            this.context = context;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            // 加载Item的布局.布局中用到的真正的CardView.
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.main_item, viewGroup, false);
            // ViewHolder参数一定要是Item的Root节点.
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder viewHolder, int i) {
            //从data中获取数据，填充入视图中
            final Post post = data.get(i);
            if (post!=null){
                final String objectId = post.getObjectId();
                final String cacheBooleanKey = objectId + "footerBoolean";
                final ArrayList<Integer> numberFooter = post.getNumberFooter();
                final byte[] footerBoolean = aCache.getAsBinary(cacheBooleanKey);
                //要显示表情
                String zhengze = "f0[0-9]{2}|f10[0-7]";
                SpannableString spannableString = ExpressionUtil.getExpressionString(context, post.getContent(), zhengze);
                viewHolder.content.setText(spannableString);

                viewHolder.userName.setText(post.getUser().getUsername());// 级联查询查找username
                viewHolder.merchantName.setText(post.getMerchantName());

                viewHolder.tvShared.setText("享受过" + (numberFooter.get(SHARE)));
                viewHolder.tvLike.setText("喜欢"+(numberFooter.get(LIKE)));
                viewHolder.tvDislike.setText("没有帮助" + (numberFooter.get(DISLIKE)));
                viewHolder.tvConment.setText("评论"+ numberFooter.get(COMMENT));

                viewHolder.container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, DeatilActivity.class);
                        intent.putExtra("post",post);
                        context.startActivity(intent);

                    }
                });

                /**
                 *定义菜单时间
                 */
                viewHolder.list.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MyUtils.showPopupMenu(context);
                    }
                });


                /**
                 * 享受过
                 */
                viewHolder.shared.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MyUtils.footerCommand(footerBoolean,SHARE,viewHolder.ivShare,viewHolder.tvShared,numberFooter,aCache,post,context,objectId);
                    }
                });

                /**
                 * 喜欢
                 */
                viewHolder.like.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MyUtils.footerCommand(footerBoolean,LIKE,viewHolder.ivLike,viewHolder.tvLike,numberFooter,aCache,post,context,objectId);
                    }
                });

                /**
                 * 没有帮助
                 */
                viewHolder.dislike.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MyUtils.footerCommand(footerBoolean,DISLIKE,viewHolder.ivDislike,viewHolder.tvDislike,numberFooter,aCache,post,context,objectId);
                    }
                });

                /**
                 * 评论
                 */
                viewHolder.comment.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //得到当前的post
                        currentPost = post;
                        InputMethodManager imm= (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        llFoot.setVisibility(View.VISIBLE);
                        comment.requestFocus();
                        //强制显示键盘
                        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                    }
                });

                /**
                 * 分享事件
                 */
                viewHolder.share.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, "我有好东西与您分享"+post.getContent());
                        sendIntent.setType("text/plain");
                        context.startActivity(sendIntent);
                    }
                });

                //点击头像进入个人主页
                viewHolder.avata.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, PersonalActivity.class);
                        intent.putExtra("post",post);
                        intent.putExtra("user",post.getUser());
                        context.startActivity(intent);
                    }
                });
                //点击姓名进入个人主页
                viewHolder.userName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, PersonalActivity.class);
                        intent.putExtra("user",post.getUser());
                        context.startActivity(intent);
                    }
                });

                /**
                 * 设置展示的时候的图标颜色值，要正确的显示
                 */
            Log.e(TAG, Arrays.toString(footerBoolean));
                if (footerBoolean!=null){
                    if (footerBoolean[DISLIKE]==0){
                        viewHolder.ivDislike.setImageResource(R.drawable.icon_dislike_pressed);
                    }
                    if (footerBoolean[LIKE]==0){
                        viewHolder.ivLike.setImageResource(R.drawable.icon_heart_pressed);
                    }
                    if (footerBoolean[SHARE]==0){
                        viewHolder.ivShare.setImageResource(R.drawable.thumbs_up_pressed);
                    }
                }



                //格式化时间
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = null;
                try {
                    date = simpleDateFormat.parse(post.getCreatedAt());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                viewHolder.time.setText(MyUtils.timeLogic(date,context));

                //获取图片，使用Picasso可以缓存
                final String paths [] = post.getPaths().split("\\|");
                int t = paths.length;
                Log.e(TAG, String.valueOf(t));


                final ArrayList<String> arrayList = post.getThumnailsName();
                Log.e(TAG,arrayList.toString());

//                for (int i1 = 0 ;i1 <paths.length; i1++) {
//
//                    final ImageView imageView;
//                    imageView = new ImageView(context);
//                    picasso.load(paths[i1]).placeholder(R.drawable.ic_launcher).resize(200, 200).into(imageView);
//                    imageView.setPadding(2, 2, 2, 2);
//                    imageView.setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            Dialog dialog = new Dialog(context);
//                            View photoView = LayoutInflater.from(context).inflate(R.layout.photo_view,null);
//                            ImageView photo = (ImageView) photoView.findViewById(R.id.photo);
//                            photo.setImageDrawable(imageView.getDrawable());
//                            dialog.setContentView(photoView);
//                            dialog.show();
//                        }
//                    });
//                    viewHolder.gallery.addView(imageView);
//                }

                /**
                 * 想获取缩略图，但是没有成功
                 * 问题解决了，是因为没有进行sign认证，所以没有有400错误
                 */
                //TODO
                for (int i1 = 0 ;i1 <arrayList.size(); i1++) {
                    final int finalI = i1;
                    BmobProFile.getInstance(context).submitThumnailTask(arrayList.get(i1), 1, new ThumbnailListener() {
                        @Override
                        public void onSuccess(String thumbnailName, String thumbnailUrl) {
                            ImageView imageView =new ImageView(context);
                            picasso.load(BmobProFile.getInstance(context).signURL(thumbnailName, thumbnailUrl, "54f197dc6dce11fc7c078c07420a080e", 0, null)).placeholder(R.drawable.ic_launcher).resize(200, 200).into(imageView);
                            imageView.setPadding(2,2,2,2);
                            viewHolder.gallery.addView(imageView);
                            imageView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(context, GalleryUrlActivity.class);
                                    intent.putExtra("photoUrls",paths);
                                    intent.putExtra("currentItem", finalI);
                                    context.startActivity(intent);
                                }
                            });
                        }
                        @Override
                        public void onError(int statuscode, String errormsg) {
                            Log.e(TAG, errormsg);
                        }
                    });

                }
            }

            setAnimation(viewHolder.container,i);

        }

        private void setAnimation(View viewToAnimate, int position)
        {
            // If the bound view wasn't previously displayed on screen, it's animated
            if (position > lastPosition)
            {
                Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left);
                viewToAnimate.startAnimation(animation);
                lastPosition = position;
            }
        }
        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView merchantName,userName,content,time,tvShared,tvLike,tvDislike,tvConment;
            ImageView avata,share,list,ivShare,ivLike,ivDislike,ivComment;
            LinearLayout shared,like,dislike,comment,gallery;
            CardView container;

            public ViewHolder(View itemView) {
                // super这个参数一定要注意,必须为Item的根节点.否则会出现莫名的FC.
                super(itemView);
                merchantName = (TextView) itemView.findViewById(R.id.tv_main_merchantName);
                userName = (TextView) itemView.findViewById(R.id.tv_main_postName);
                content = (TextView) itemView.findViewById(R.id.tv_main_content);
                time = (TextView) itemView.findViewById(R.id.tv_main_time);
                avata = (ImageView) itemView.findViewById(R.id.iv_main_logo);
                shared = (LinearLayout) itemView.findViewById(R.id.ll_main_shared);
                like = (LinearLayout) itemView.findViewById(R.id.ll_main_like);
                dislike = (LinearLayout) itemView.findViewById(R.id.ll_main_dislike);
                comment = (LinearLayout) itemView.findViewById(R.id.ll_main_conment);
                tvShared = (TextView) itemView.findViewById(R.id.tv_main_shared);
                tvLike = (TextView) itemView.findViewById(R.id.tv_main_like);
                tvDislike = (TextView) itemView.findViewById(R.id.tv_main_dislike);
                tvConment = (TextView) itemView.findViewById(R.id.tv_main_comment);
                share = (ImageView) itemView.findViewById(R.id.iv_main_share);
                list = (ImageView) itemView.findViewById(R.id.iv_main_list);
                gallery = (LinearLayout) itemView.findViewById(R.id.ll_gallery);
                ivComment = (ImageView) itemView.findViewById(R.id.iv_main_comment);
                ivDislike = (ImageView) itemView.findViewById(R.id.iv_main_dislike);
                ivLike = (ImageView) itemView.findViewById(R.id.iv_main_like);
                ivShare = (ImageView) itemView.findViewById(R.id.iv_main_shared);

                container = (CardView) itemView.findViewById(R.id.container);
            }
        }
    }



}
