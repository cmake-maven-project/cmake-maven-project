package org.bitbucket.cowwoc.cmake;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.maven.plugin.logging.Log;

/**
 * A source-code compiler.
 *
 * @author Gili Tzabari
 */
public abstract class Compiler
{
  /**
   * Project file types.
   */
  public enum Type
  {
    /**
     * Borland makefiles.
     */
    BORLAND,
    /**
     * MSYS makefiles.
     */
    MSYS,
    /**
     * MinGW.
     *
     * @see http://www.mingw.org/
     */
    MINGW,
    /**
     * NMake makefiles.
     */
    NMAKE,
    /**
     * Qt JOM makefiles.
     *
     * @see http://labs.qt.nokia.com/2009/03/27/speeding-up-visual-c-qt-builds/
     */
    JOM,
    /**
     * GNU makefiles.
     *
     * @see http://www.gnu.org/software/make/
     */
    GNU_MAKE,
    /**
     * Visual studio project files.
     */
    VISUAL_STUDIO,
    /**
     * Watcom project files.
     *
     * @see http://en.wikipedia.org/wiki/Open_Watcom
     */
    WATCOM,
    /**
     * Code::Blocks project files.
     *
     * @see http://www.codeblocks.org/
     */
    CODE_BLOCKS,
    /**
     * Eclipse project files.
     *
     * @see http://www.eclipse.org/
     */
    ECLIPSE
  }
  private final Type type;
  private final File path;
  private final Log log;

  /**
   * Creates a new Compiler.
   *
   * @param type the compiler type
   * @param path the compiler path
   * @param log Maven log
   */
  protected Compiler(Type type, File path, Log log)
  {
    this.type = type;
    this.path = path;
    this.log = log;
  }

  /**
   * Creates a Compiler from a makefile generator name.
   * 
   * @param name the makefile generator
   * @param log the Maven log
   * @return the Compiler associated with the makefile generator
   */
  public static Compiler fromGenerator(String name, Log log)
  {
    VisualStudioCompiler vs6 = new VisualStudioCompiler(VisualStudioCompiler.Version.V6, log);
    VisualStudioCompiler vs70 = new VisualStudioCompiler(VisualStudioCompiler.Version.V7_0, log);
    VisualStudioCompiler vs71 = new VisualStudioCompiler(VisualStudioCompiler.Version.V7_1, log);
    VisualStudioCompiler vs8 = new VisualStudioCompiler(VisualStudioCompiler.Version.V8, log);
    VisualStudioCompiler vs9 = new VisualStudioCompiler(VisualStudioCompiler.Version.V9, log);
    VisualStudioCompiler vs10 = new VisualStudioCompiler(VisualStudioCompiler.Version.V10, log);
    Map<String, Compiler> compilerNameToType = new ImmutableMap.Builder<String, Compiler>().put(
      "Visual Studio 6", vs6).
      put("Visual Studio 7", vs70).
      put("Visual Studio 7 .NET 2003", vs71).
      put("Visual Studio 8", vs8).
      put("Visual Studio 8 Win64", vs8).
      put("Visual Studio 9", vs9).
      put("Visual Studio 9 Win64", vs9).
      put("Visual Studio 10", vs10).
      put("Visual Studio 10 Win64", vs10).build();
    return compilerNameToType.get(name);
  }

  /**
   * Returns the compiler path.
   *
   * @return the compiler path
   */
  public File getPath()
  {
    return path;
  }

  /**
   * Returns the compiler type.
   *
   * @return the compiler type
   */
  public Type getType()
  {
    return type;
  }

  /**
   * Returns the Maven log.
   *
   * @return the Maven log
   */
  protected Log getLog()
  {
    return log;
  }

  /**
   * Returns cmake's name for this compiler.
   *
   * @return cmake's name for this compiler
   */
  public abstract String getName();

  /**
   * Compiles a project.
   *
   * @param projectPath the project file
   * @param platform i386, amd64, etc
   * @param buildType debug, release, etc
   * @throws IOException if an I/O error occurs while compiling a project
   * @throws InterruptedException if the thread was interrupted
   * @return true on success
   */
  public abstract boolean compile(File projectPath, String platform, String buildType)
    throws IOException, InterruptedException;

  @Override
  public String toString()
  {
    return getClass().getName() + "[path=" + getPath() + "]";
  }
}
