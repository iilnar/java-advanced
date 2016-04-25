package ru.ifmo.ctddev.sabirzyanov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;
import org.apache.commons.io.IOUtils;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * This class creates implementation of classes, that you provide.
 *
 * @author Ilnar
 */
public class Implementor implements Impler, JarImpler {
    private static String LS = System.lineSeparator();
    private static String TAB = "    ";
    private StringBuilder sb = new StringBuilder();

    /**
     *
     * Main method to execute in this class
     * <p>
     * If number of the arguments is valid, what means more or equal two,
     * this will command to make implementation of class given as the first argument
     * and whether the argument {@code -jar} is given the implementation will be
     * archieved to a JAR file
     *
     * @param args arguments from command line
     * @see #implement(Class, Path)
     * @see #implementJar(Class, Path)
     */
    public static void main(String[] args) {
        if (args == null || args.length < 2) {
            System.err.println("Not enough arguments! Must be at least 2");
            System.err.println("Usage: \"-jar\" <ClassName> <JarName>");
            System.err.println("Usage: <ClassName> <Directory>");
            return;
        }
        Implementor impl = new Implementor();
        boolean isJar = args[0].equals("-jar");
        try {
            Class<?> c = Class.forName(args[(isJar) ? (1) : (0)]);
            if (isJar) {
                impl.implementJar(c, Paths.get(args[2]) );
            } else {
                impl.implement(c, Paths.get(args[1]));
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Cannot find class: " + args[(isJar) ? (1) : (0)]);
        } catch (ImplerException e) {
            System.err.println("Cannot implement class: " + args[(isJar) ? (1) : (0)] + " cause: " + e.getMessage());
        }

    }

    /**
     * Returns default value of class type
     * @param cl class to implement
     * @return default value of {@code cl}
     */
    private String defaultFValue(Class<?> cl) {
        if (!cl.isPrimitive()) {
            return "null";
        }
        if (cl.equals(boolean.class)) {
            return "false";
        }
        if (cl.equals(void.class)) {
            return "";
        }
        return "0";
    }

    /**
     * Method to generate non-jar implementation.
     *
     * Creates a file that correctly implements or extends interface or class.
     * Output file if created in the folder that corresponds to the package of
     * the given class or interface. Output file contains java class that implements
     * or extends given class or interface.
     * Generics aren't allowed.
     * @param token target class or interface, which is going to be implemented.
     * @param root directory where implementation should be placed to.
     * * @throws ImplerException if {@code aClass} is final or got no non-private constructors
     * or cannot create directories according to {@code aCLass}'s package
     * @see Impler
     * @see #implementJar(Class, Path)
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        sb.setLength(0);
        if (token == null || root == null) {
            throw new ImplerException("One or more arguments are null");
        }
        if (Modifier.isFinal(token.getModifiers())) {
            throw new ImplerException("Can't implement final class");
        }
        final String className = token.getSimpleName() + "Impl";
        final String classCanName = token.getCanonicalName();
        if (token.getPackage() != null) {
            sb
                    .append("package ")
                    .append(token.getPackage().getName())
                    .append(";")
                    .append(LS)
                    .append(LS);
        }
        sb
                .append("public class ")
                .append(className);
        if (token.isInterface()) {
            sb.append(" implements");
        } else {
            sb.append(" extends");
        }
        sb
                .append(" ")
                .append(classCanName)
                .append(" {")
                .append(LS);
        int consCnt = 0;
        Constructor<?>[] constructors = token.getDeclaredConstructors();
        for (Constructor<?> cons : constructors) {
            if (!Modifier.isPrivate(cons.getModifiers())) {
                implConstructor(cons, className);
                consCnt++;
            }
        }
        if (consCnt == 0 && constructors.length != 0) {
            throw new ImplerException("No non public constructor");
        }
        Set<Method> methods = collectAbstractMethods(token);
        methods.forEach(this::implMethods);
        sb.append("}").append(LS);
        try (BufferedWriter out = Files.newBufferedWriter(getFilePath(token, root))) {
            out.write(sb.toString());
        } catch (IOException e) {
            throw new ImplerException("Class " + token.getName() + " can't be printed");
        }
    }

    /**
     * Returns {@link Path} to the {@code token} file in {@code path}.
     * @param token class to be implemented.
     * @param path directory where class should be implemented.
     * @return {@link Path} to the {@code token}
     * @throws IOException if creation of directories was failed.
     */
    private Path getFilePath(Class token, Path path) throws IOException {
        if (token.getPackage() != null) {
            Path p = Paths.get(path.toString(), token.getPackage().getName().replace(".", File.separator));
            Files.createDirectories(p);
            path = p;
        }
        return Paths.get(path.toString(), token.getSimpleName() + "Impl.java");
    }

    /**
     * Appends modifier to {@code sb}.
     * @param mod {@link Modifier} of instance.
     * @param type {@link} type of instance.
     */
    private void modifiers(int mod, int type) {
        sb.append(Modifier.toString(mod & ~Modifier.ABSTRACT & type));
    }

    /**
     * Append parameters to the {@code sb}.
     * @param params parameters list.
     */
    private void parameters(Parameter[] params) {
        for (int i = 0; i < params.length; i++) {
            if (0 < i) {
                sb.append(", ");
            }
            modifiers(params[i].getModifiers(), Modifier.parameterModifiers());
            sb
                    .append(" ")
                    .append(params[i].getType().getCanonicalName())
                    .append(" arg")
                    .append(i);
        }
    }

    /**
     * Appends exceptions to {@code sb}.
     * @param excep throwable exceptions list.
     */
    private void exceptions(Class[] excep) {
        if (excep.length != 0) {
            sb.append(" throws ");
            for (int i = 0; i < excep.length; i++) {
                if (0 < i) {
                    sb.append(", ");
                }
                sb.append(excep[i].getCanonicalName());
            }
        }
    }

    /**
     * Implements {@code cons} constructor. Result appended to {@code sb}.
     * @param cons constructor to be implemented.
     * @param className name of class, for which constructor is implemented.
     */
    private void implConstructor(Constructor<?> cons, String className) {
        sb.append(TAB);
        modifiers(cons.getModifiers(), Modifier.constructorModifiers());
        sb.append(" ").append(className).append("(");
        Parameter[] params = cons.getParameters();
        parameters(params);
        sb
                .append(")");
        exceptions(cons.getExceptionTypes());
        sb
                .append(" {")
                .append(LS)
                .append(TAB)
                .append(TAB)
                .append("super(");
        for (int i = 0; i < params.length; i++) {
            if (0 < i) {
                sb.append(", ");
            }
            sb
                    .append("arg")
                    .append(i);
        }
        sb
                .append(");")
                .append(LS)
                .append(TAB)
                .append("}")
                .append(LS)
                .append(LS);
    }

    /**
     * Implements {@code method}. Result appended to {@code sb}.
     * @param method method to be implemented.
     */
    private void implMethods(Method method) {
        sb.append(TAB);
        modifiers(method.getModifiers(), Modifier.methodModifiers());
        sb
                .append(" ")
                .append(method.getReturnType().getCanonicalName())
                .append(" ")
                .append(method.getName())
                .append("(");
        parameters(method.getParameters());
        sb.append(") ");
        exceptions(method.getExceptionTypes());
        sb
                .append(" {")
                .append(LS)
                .append(TAB)
                .append(TAB)
                .append("return ")
                .append(defaultFValue(method.getReturnType()))
                .append(";")
                .append(LS)
                .append(TAB)
                .append("}")
                .append(LS)
                .append(LS);
    }

    /**
     *
     * Provides implementation of class archieved in JAR
     * <p>
     * Takes given class. Makes implementation and tries to compile it.
     * If the compilation succedes tries to make JAR file of the compiled
     * byte-code.
     *
     * @param token class to implement.
     * @param jarFile JAR to implement to.
     * @throws ImplerException if compilation fails or {@code implement} fails.
     * @see #implement(Class, Path)
     */
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (token == null || jarFile == null) {
            throw new ImplerException("One or more arguments are null");
        }
        Path path;
        try {
            path = Files.createTempDirectory("TempRoot");
        } catch (IOException e) {
            throw new ImplerException("Couldn't create temp directory");
        }
        implement(token, path);
        String className = File.separator + token.getSimpleName() + "Impl";
        if (token.getPackage() != null) {
            className = token.getPackage().getName().replace(".", File.separator) + className;
        }
        int exitCode = compile(path, path + File.separator + className + ".java");
        if (exitCode != 0) {
            throw new ImplerException("Compilation error");
        }
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (FileInputStream fileInputStream = new FileInputStream(path + File.separator + className + ".class");
             JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarFile.toFile()), manifest)
        ) {
            jarOutputStream.putNextEntry(new ZipEntry(className + ".class"));
//            IOUtils.copy(fileInputStream, jarOutputStream);
            byte buffer[] = new byte[1024];
            int read;
            while ((read = fileInputStream.read(buffer)) > 0) {
                jarOutputStream.write(buffer, 0, read);
            }
            jarOutputStream.closeEntry();
        } catch (IOException e) {
            throw new ImplerException("Couldn't make jar file cause " + e.getMessage());
        }
    }

    /**
     * Provides basic usage of Java compiler.
     * <p>
     * Requests an system Java compiler and passes {@code file} to it.
     * After that returns result of compilation.
     *
     * @param path to source
     * @param file name of class to compile
     * @return exitcode of compiler
     */
    private int compile(Path path, String file) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<String> args = new ArrayList<>();
        args.add(file);
        args.add("-cp");
        args.add(path + File.pathSeparator + System.getProperty("java.class.path"));
        return compiler.run(null, System.err, System.err, args.toArray(new String[args.size()]));
    }


    /**
     * Helper class for collecting methods by own hash.
     */
    private class MethodHashed {
        public final Method m;

        private MethodHashed(Method m) {
            this.m = m;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MethodHashed)) {
                return false;
            }
            Method m2 = ((MethodHashed) obj).m;

            return m.getName().equals(m2.getName()) && Arrays.equals(m.getParameterTypes(), m2.getParameterTypes());
        }

        @Override
        public int hashCode() {
            int hash = m.getName().hashCode();
            for (Parameter p : m.getParameters()) {
                hash ^= p.getType().hashCode();
            }
            return hash;
        }
    }

    /**
     *
     * Method to walk all ancestors of the class
     * <p>
     * This method will walk to very beginning of {@code clazz} inheritance hierarchy and
     * fill up dictionary with method, which are abstract and non-private.
     *
     * @param clazz target class
     * @return {@link Set} of methods.
     */
    private Set<Method> collectAbstractMethods(Class<?> clazz) {
        Set<MethodHashed> abstractMethods = new HashSet<>();
        if (clazz == null || !Modifier.isAbstract(clazz.getModifiers())) {
            return Collections.emptySet();
        }
        for (Method m : clazz.getMethods()) {
            if (Modifier.isAbstract(m.getModifiers())) {
                abstractMethods.add(new MethodHashed(m));
            }
        }
        while (clazz != null) {
            if (!Modifier.isAbstract(clazz.getModifiers())) {
                break;
            }
            for (Method m : clazz.getDeclaredMethods()) {
                int mod = m.getModifiers();
                if (!Modifier.isPrivate(mod) && !Modifier.isPublic(mod)) {
                    abstractMethods.add(new MethodHashed(m));
                }
            }
            clazz = clazz.getSuperclass();
        }
        abstractMethods.removeIf(method -> !Modifier.isAbstract(method.m.getModifiers()));
        return abstractMethods.stream().map(box -> box.m).collect(Collectors.toSet());
    }
}