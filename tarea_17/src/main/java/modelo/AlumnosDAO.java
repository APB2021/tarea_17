package modelo;

public interface AlumnosDAO {

	// ALUMNOS:

	public boolean insertarAlumno(Alumno alumno);

	public Alumno solicitarDatosAlumno();

	public boolean mostrarTodosLosAlumnos(boolean mostrarTodaLaInformacion);

	public boolean modificarNombreAlumnoPorNIA(int nia, String nuevoNombre);

	public boolean eliminarAlumnoPorNIA(int nia);

	boolean mostrarAlumnoPorNIA(int nia);

	public boolean eliminarAlumnosPorApellidos(String apellidos);

	// FICHEROS:

	public void guardarAlumnosEnFicheroTexto();

	public boolean leerAlumnosDeFicheroTexto();

	public void guardarAlumnosEnFicheroJSON();

	public boolean leerAlumnosDeFicheroJSON();

	// GRUPOS:

	public boolean insertarGrupo(Grupo grupo);

	public boolean eliminarAlumnosPorGrupo(String grupo);

	public void guardarGruposEnFicheroJSON();

	public boolean leerGruposDeFicheroJSON();

	/**
	 * Guarda todos los grupos y sus alumnos en un archivo XML llamado 'grupos.xml'.
	 * Si el archivo ya existe, solicita confirmación al usuario antes de
	 * sobrescribirlo.
	 * 
	 * @return true si el archivo se guarda correctamente, false si ocurre un error.
	 */
	boolean guardarGruposEnXML();

	/**
	 * Lee un archivo XML que contiene información sobre grupos y alumnos, y guarda
	 * los datos en las tablas correspondientes de la base de datos.
	 *
	 * @param rutaArchivo Ruta del archivo XML a procesar.
	 * @return true si los datos fueron procesados e insertados correctamente, false
	 *         en caso de error.
	 */
	boolean leerYGuardarGruposXML(String rutaArchivo);

	/**
	 * Muestra todos los alumnos del grupo seleccionado por el usuario.
	 */
	void mostrarAlumnosPorGrupo();

	/**
	 * Cambia el grupo de un alumno seleccionado por el usuario.
	 * 
	 * @return true si el cambio se realizó correctamente, false en caso de error.
	 */
	boolean cambiarGrupoAlumno();

	/**
	 * Guarda un grupo específico con toda su información (incluyendo los alumnos)
	 * en un archivo XML. Solicita al usuario el nombre del grupo.
	 * 
	 * @return true si el archivo se guarda correctamente, false si ocurre un error.
	 */
	boolean guardarGrupoEspecificoEnXML();
}