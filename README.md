#自定义控件-抽奖转盘
![](http://i11.tietuku.com/950e7392e90def8e.gif)

1. 这个自定义控件我们使用surfaceview实现，记得之前的卫星菜单控件使用viewgroup实现，这个为什么使用surfaceview实现呢，他俩之间有何不同呢？
	
	- surfaceview继承自view，但是view的绘制是在UI线程中进行的，而surfaceview是在子线程中进行绘制的，这就说明了surfaceview适合于频繁绘制的情况，抽奖转盘要不停的绘制来实现旋转效果，所以我们使用surfaceview。
	- 现在我们知道surfaceview是在子线程中绘制的，那么surfaceview是如何拿到canvas的呢？通过surfaceview中的getHolder拿到surfaceHolder，通过holder拿到canvas。
	- 我们知道view的ondraw方法用于绘制，这个方法是系统回调的，那么使用surfaceview是在何时绘制呢？其实，holder不仅可以拿到canvas，还管理着surfaceview的生命周期，其生命周期有三个方法surfaceCreate，surfaceChanged，surfaceDestoryed，我们可以在surfaceCreate中创建子线程，在子线程中绘制，最后在surfaceDestoryed中关闭子线程。
2. 下面我们解释具体步骤：

	**(1). 在activity_main的布局文件中使用**

	    <com.example.zhaimeng.imooc_luckpan.LuckyPan
        android:id="@+id/id_luckypan"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_centerInParent="true"
        android:padding="30dp"></com.example.zhaimeng.imooc_luckpan.LuckyPan>
	**(2). 控件的类** 这其中有几个方法：

		onMeasure
		surfaceCreated
		surfaceDestroyed
	onMeasure来测量自己，首先我们将整个转盘视为正方形，它在屏幕中以正方形摆放在中间，我们在onMeasure方法中用setMeasuredDimension(width,width)实现。
	
	在surfaceCreated方法中，首先先拿到画笔Paint（包括画字盘块的画笔，画文字的画笔），最重要的是创建一个线程，start它。

	在run方法中每50ms执行一次draw方法，draw内容为：
	1. 通过holder拿到Canvas，绘制背景图片(drawBitmap)，绘制文字(drawTextOnPath)，绘制奖项的图片(drawBitmap)。
	2. 控制转盘速度，控件转盘启停等逻辑操作，并在这里回调抽完奖后的回调方法。
	
	**(3). 转盘的旋转**
	- 定义一个speed值，每draw一次speed减1并且每次转的角度量为speed值，这样speed会从初始值变为0，而转盘没经历一次draw都会转比上一次小一个speed的角度（比如第一次draw转盘转50，第二次转49，48...直到0），到0后就逻辑判断使其停止，这样就实现了转盘的旋转。
	- 作弊：使每次抽奖能抽到什么是由我们给的参数决定的。 
		![](http://i4.tietuku.com/dbfa2face05c8780.jpg)  
##总结  
1.这个控件是不断的旋转，需要不断的绘制，需要使用surfaceview来做，surfaceview的主要特点就是它具有独立的surface并且可以在子线程中绘制，不会影响UI线程响应与用户的交互。  
2.surfaceview的生命周期有三个方法`surfaceCreated, surfaceChanged, surfaceDestroyed`  
3.控件的构造方法中拿到自定义属性  
4.开启子线程，并不断的循环执行draw方法，然后绘制盘块，绘制文字，绘制中奖图片，最后使转盘的偏移角度增加，并判断是否按下了停止按钮，按下后转盘每次转的角度变小，直到变为0（不转了）就停止了。  
5.有了以上的基础基本上就可以是转盘转动并且停止了，但是这里我们要实现获得什么奖品是后台决定的，并不是随机的。  
6.要实现作弊效果其实就是由停止位置来确定开始转动时的启动速度（因为当按下停止按钮时转盘是匀速减速的，所以可以根据停止位置来确定开始速度），只要启动速度确定了（取左边沿至右边沿的一个随机值），其停止时的位置就能确定（这里当按下停止时是固定的让开始角度置为0，也就是回到了初始状态，这样才能由起始速度确定停止位置）  

	
	
