package modelo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Consumer;

public class AlumnosFichero implements AlumnosDAO {

	@Override
	public boolean insertarAlumno(Connection conexionBD, Alumno alumno) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean mostrarTodosLosAlumnos(Connection conexionBD, boolean mostrarTodaLaInformacion) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void guardarAlumnosEnFicheroTexto(Connection conexionBD) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean leerAlumnosDeFicheroTexto(Connection conexionBD) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean modificarNombreAlumnoPorNIA(Connection conexion, int nia, String nuevoNombre) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean eliminarAlumnoPorNIA(Connection conexionBD, int nia) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean eliminarAlumnosPorApellidos(Connection conexionBD, String apellidos) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void guardarAlumnosEnFicheroJSON(Connection conexionBD) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean leerAlumnosDeFicheroJSON(Connection conexionBD) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean insertarGrupo(Connection conexionBD, Grupo grupo) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean eliminarAlumnosPorGrupo(Connection conexionBD, String grupo) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void guardarGruposEnFicheroJSON(Connection conexionBD) throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean leerGruposDeFicheroJSON(Connection conexionBD) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Alumno solicitarDatosAlumno() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean mostrarAlumnoPorNIA(Connection conexionBD, int nia) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean ejecutarOperacionConNIA(Connection conexionBD, String sql,
			Consumer<PreparedStatement> configuracionParams) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

}
