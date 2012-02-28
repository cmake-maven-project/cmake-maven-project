package com.googlecode.cmakemavenproject;

import java.io.File;
import java.io.IOException;
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
	private final Log log;

	/**
	 * Creates a new Compiler.
	 *
	 * @param type the compiler type
	 * @param log Maven log
	 */
	protected Compiler(Type type, Log log)
	{
		this.type = type;
		this.log = log;
	}

	/**
	 * Creates a Compiler from a makefile generator name.
	 *
	 * @param name the makefile generator
	 * @param log the Maven log
	 * @return null if the name does not map to a known compiler
	 */
	public static Compiler fromGenerator(String name, Log log)
	{
		Compiler result = VisualStudioCompiler.fromGenerator(name, log);
		if (result != null)
			return result;
		result = GccCompiler.fromGenerator(name, log);
		if (result != null)
			return result;
		return null;
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
}
