package fm.jiecao.jiecaovideoplayer;

/**
 * Created by LiaoHongjie on 2017/8/12.
 */

class A{

    public void f(){
        System.out.print("A中f");
    }

    public void g(){
        f();
    }

}

class B extends A{

    public void f(){
        System.out.print("B中f");
    }
}



public class Test {

    public static void main(String[] args){

        B b = new B();
        b.g();

    }
}
