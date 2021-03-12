package nl.cornerstone.bytebuddy;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.junit.Assert.assertEquals;

import java.util.function.IntSupplier;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.*;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.*;
import org.junit.Test;

public class Test1 {
  private static TypeDescription type(Class<?> cls) { return TypeDescription.ForLoadedType.of(cls); }
  
  static int plus(IntSupplier a, IntSupplier b) { return a.getAsInt() + b.getAsInt(); }

  @Test
  public void test1() throws Exception {
    MethodDescription.InDefinedShape plus = type(getClass()).getDeclaredMethods().filter(named("plus")).getOnly();
    int privateStatic = Visibility.PRIVATE.getMask() | Ownership.STATIC.getMask();
    TypeDescription.Generic intType = type(int.class).asGenericType();
    String targetName = getClass().getPackageName() + ".IntSupplierImpl";
    TypeDescription targetType = new TypeDescription.Latent(targetName, privateStatic, type(Object.class).asGenericType(), type(IntSupplier.class).asGenericType());
    MethodDescription.InDefinedShape one = new MethodDescription.Latent(targetType, new MethodDescription.Token("one", privateStatic, intType));
    MethodDescription.InDefinedShape two = new MethodDescription.Latent(targetType, new MethodDescription.Token("two", privateStatic, intType));

    IntSupplier answer
        = new ByteBuddy()
        .subclass(IntSupplier.class)
        .name(targetName)

        .defineMethod("one", int.class, privateStatic)
        .intercept(FixedValue.value(20))

        .defineMethod("two", int.class, privateStatic)
        .intercept(FixedValue.value(22))

        .defineMethod("getOne", IntSupplier.class, privateStatic)
        .intercept(InvokeDynamic.lambda(one, type(IntSupplier.class)))

        .defineMethod("getTwo", IntSupplier.class, privateStatic)
        .intercept(InvokeDynamic.lambda(two, type(IntSupplier.class)))

        .method(named("getAsInt"))
        .intercept(
            MethodCall.invoke(plus)
            .withMethodCall(MethodCall.invoke(named("getOne")))
            .withMethodCall(MethodCall.invoke(named("getTwo"))))

        .make()
        .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
        .getLoaded()
        .getConstructor()
        .newInstance();

    assertEquals(42, answer.getAsInt());
  }
}