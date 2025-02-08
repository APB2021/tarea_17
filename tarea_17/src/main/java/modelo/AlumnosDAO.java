package modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Consumer;

public interface AlumnosDAO {

	// ALUMNOS:

	public boolean insertarAlumno(Connection conexionBD, Alumno alumno) throws SQLException;

	public Alumno solicitarDatosAlumno() throws SQLException;

	public boolean mostrarTodosLosAlumnos(Connection conexionBD, boolean mostrarTodaLaInformacion) throws SQLException;

	public void guardarAlumnosEnFicheroTexto(Connection conexionBD) throws SQLException;

	public boolean leerAlumnosDeFicheroTexto(Connection conexionBD) throws SQLException;

	public boolean ejecutarOperacionConNIA(Connection conexionBD, String sql,
			Consumer<PreparedStatement> configuracionParams) throws SQLException;

	public boolean modificarNombreAlumnoPorNIA(Connection conexion, int nia, String nuevoNombre) throws SQLException;

	public boolean eliminarAlumnoPorNIA(Connection conexionBD, int nia) throws SQLException;

	boolean mostrarAlumnoPorNIA(Connection conexionBD, int nia) throws SQLException;

	public boolean eliminarAlumnosPorApellidos(Connection conexionBD, String apellidos) throws SQLException;

	public void guardarAlumnosEnFicheroJSON(Connection conexionBD) throws SQLException;

	public boolean leerAlumnosDeFicheroJSON(Connection conexionBD) throws SQLException;

	// GRUPOS:

	public boolean insertarGrupo(Connection conexionBD, Grupo grupo) throws SQLException;

	public boolean eliminarAlumnosPorGrupo(Connection conexionBD, String grupo) throws SQLException;

	public void guardarGruposEnFicheroJSON(Connection conexionBD) throws SQLException;

	public boolean leerGruposDeFicheroJSON(Connection conexionBD) throws SQLException;

}