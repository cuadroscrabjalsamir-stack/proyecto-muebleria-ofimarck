package com.ofimarck.service;

import com.ofimarck.domain.Categoria;
import com.ofimarck.dto.response.CategoriaDTO;
import com.ofimarck.exception.BadRequestException;
import com.ofimarck.exception.ResourceNotFoundException;
import com.ofimarck.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<Categoria> listarActivas() {
        return categoriaRepository.findByEstadoTrueOrderByNombreAsc();
    }

    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }

    public Categoria buscarPorId(Integer id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + id));
    }

    @Transactional
    public Categoria crear(CategoriaDTO dto) {
        if (categoriaRepository.existsByNombreIgnoreCase(dto.getNombre())) {
            throw new BadRequestException("Ya existe una categoría con el nombre: " + dto.getNombre());
        }
        String slug = generarSlug(dto.getNombre());
        if (categoriaRepository.existsBySlugIgnoreCase(slug)) {
            slug = slug + "-" + System.currentTimeMillis() % 1000;
        }

        Categoria c = new Categoria();
        c.setNombre(dto.getNombre().trim());
        c.setSlug(slug);
        c.setDescripcion(dto.getDescripcion());
        c.setEstado(dto.getEstado() != null ? dto.getEstado() : true);
        return categoriaRepository.save(c);
    }

    @Transactional
    public Categoria actualizar(Integer id, CategoriaDTO dto) {
        Categoria c = buscarPorId(id);
        if (!c.getNombre().equalsIgnoreCase(dto.getNombre()) &&
                categoriaRepository.existsByNombreIgnoreCase(dto.getNombre())) {
            throw new BadRequestException("Ya existe otra categoría con el nombre: " + dto.getNombre());
        }
        c.setNombre(dto.getNombre().trim());
        c.setDescripcion(dto.getDescripcion());
        if (dto.getEstado() != null) {
            c.setEstado(dto.getEstado());
        }
        return categoriaRepository.save(c);
    }

    @Transactional
    public Categoria cambiarEstado(Integer id, Boolean nuevoEstado) {
        Categoria c = buscarPorId(id);
        c.setEstado(nuevoEstado);
        return categoriaRepository.save(c);
    }

    public static String generarSlug(String input) {
        if (input == null)
            return "";
        String nowhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
