package br.com.als.schema;

import java.util.Objects;
import java.util.Set;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@ProtoDoc("@Indexed")
public class SalariosOrfaos {

   private Integer cd_cli;
   private Set<SalarioCDC> salarios;

   @ProtoFactory
   public SalariosOrfaos(Integer cd_cli, Set<SalarioCDC> salarios) {
      this.cd_cli = Objects.requireNonNull(cd_cli);
      this.salarios = salarios;
   }

   public SalariosOrfaos() {
   }

   @ProtoField(1)
   public Integer getCd_cli() {
      return cd_cli;
   }

   public void setCd_cli(Integer cd_cli) {
      this.cd_cli = cd_cli;
   }

   @ProtoField(2)
   public Set<SalarioCDC> getSalarios() {
      return salarios;
   }

   public void setSalarios(Set<SalarioCDC> salarios) {
      if (salarios.stream().allMatch(sal -> sal.getCd_cli().intValue() == this.cd_cli.intValue())) {
         this.salarios = salarios;
      }
   }

   public void addOrUpdateSalario(SalarioCDC salario) {
      if ( this.salarios == null ){
         this.salarios = Set.of(salario);
         return;
      }
      
      if (this.cd_cli.intValue() == salario.getCd_cli().intValue()) {
       this.salarios
             .removeIf(sal -> sal.getCd_cli().intValue() == salario.getCd_cli().intValue() 
                           && sal.getAnoMes().intValue() == salario.getAnoMes().intValue());
         this.salarios.add(salario);
      }

   }

   @Override
   public String toString() {
      return "SalariosOrfaos [cd_cli=" + cd_cli + ", salarios=" + salarios
            + "]";
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;
      SalariosOrfaos that = (SalariosOrfaos) o;
      return Objects.equals(cd_cli, that.cd_cli) &&  Objects.equals(salarios, that.salarios);
   }

   @Override
   public int hashCode() {
      return Objects.hash(cd_cli, salarios);
   }

}
