package io.recruit_assist.recrugen.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "jd_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fileType;

    @Lob
    private byte[] fileData;
}