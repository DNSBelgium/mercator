package be.dnsbelgium.mercator.smtp.persistence.entities;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.util.List;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@Table(name = "extension")
public class ExtensionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  private String name;

  @ManyToMany(mappedBy = "supportedExtensions")
  private List<SmtpHostEntity> hosts;

  public ExtensionEntity fromString(String extension) {
    this.setName(extension);
    return this;
  }
}
