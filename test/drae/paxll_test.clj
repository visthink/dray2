(ns drae.paxll-test
  "Testing routines for Paxtools utilities."
  (:import (org.biopax.paxtools.pattern Pattern Constraint Searcher PatternBox Match)
           (org.biopax.paxtools.pattern.constraint ConBox Participant ParticipatesInConv Type PathConstraint)
           (org.biopax.paxtools.pattern.util RelType)
           )  
  (:require [clojure.test :refer :all]
            [clojure.repl :refer :all]
            [drae.util :refer [private-function]]
            [drae.paxll :refer :all]
            ))

(declare sowa-all sowa10 sowa100 sowa-set) ;; Test data -- see end of file.

(deftest bioentity-names-test
  (testing "Bioentity names test"
    (let [bioentity-name-variants (private-function bioentity-name-variants drae.paxll)]
      (is (= (bioentity-name-variants "Abc") '("Abc" #_"abc" #_"ABC")))
      (is (= (bioentity-name-variants "β-catenin") '("β-catenin" #_"Β-CATENIN" "beta-catenin")))
      )))

(deftest split-out-subnames-test
  (testing "Splitting out subnames in a composite name."
    (let [split-out-subnames (private-function split-out-subnames drae.paxll)]
      (is (= ["YAP" "TAZ"] (split-out-subnames "YAP/TAZ")))
      (is (= ["TCF" "LEF"] (split-out-subnames "TCF/LEF")))
      (is (= ["JNK"] (split-out-subnames "JNK")))
    )))

;;; Test PaxTools interface

(deftest test-get-proteins 
  (testing "Test retrieval of proteins and protein references."
    (let [m (model :raf-cascade)]
      (is (= 2 (count (all :Protein :named "cRaf" :in m))))
      (is (= 2 (count (all :Protein :named 'cRaf :in m))))  ; test w/ symbol.
      (is (contains? (.getName (first (common-generic-references 
                                        (all :Protein :named 'cRaf :in m))))
                     "RAF"))    
      )))

(deftest test-patternbox-pattern
  (testing "Test the expansion of patternbox patterns."
    (let [pattern? (private-function pattern? drae.paxll)
          xpattern? (private-function xpattern? drae.paxll)
          expand-patternbox-pattern (private-function expand-patternbox-pattern drae.paxll)
          res (expand-patternbox-pattern 'bindsTo '[con :Conversion, left :ProteinReference] "doc string")]
      (is (coll? res))
      (let [res2 (patternbox-pattern bindsTo [con :Conversion, left :ProteinReference] "doc string")]
        (is (xpattern? res2))
        ))))

(deftest test-bioentity-class
  (testing "Test the bioentity-class function call, especially classname variants."
    (let [bioentity-class (private-function bioentity-class drae.paxll)]
      (is (class? (bioentity-class :Protein)))
      (is (= (bioentity-class :Protein) (bioentity-class :protein)))
      (is (= (bioentity-class :Protein) (bioentity-class :Proteins)))
      (is (= (bioentity-class :Protein) (bioentity-class :proteins)))
      (is (= (bioentity-class :Protein) (bioentity-class "Protein")))
      (is (= (bioentity-class :Protein) (bioentity-class "Proteins")))
      (is (= (bioentity-class :Protein) (bioentity-class "proteins")))
      (is (= (bioentity-class :Protein) (bioentity-class 'Protein)))
      (is (= (bioentity-class :ProteinReference) (bioentity-class "proteinreference")))
      (is (thrown-with-msg? Exception #".+ must be a bioentity-class.+" (bioentity-class 99)))     
      (is (thrown-with-msg? Exception #"Could not find bioentity class.+" (bioentity-class :Human)))
      )))

#_(defn pattern2b []
   (let [bioentity-class (private-function bioentity-class drae.paxll)]
     (doto (Pattern. (bioentity-class :ProteinReference) "PR1")
       (add-constraints
         (erToPE PR1 P1)
         (peToControlledConv P1 Conv1)
         ((Participant. RelType/OUTPUT) Conv1 linker)
         (:not-equal linker P1)
         ((ParticipatesInConv. RelType/INPUT) linker Conv2)
         (:not-equal Conv1 Conv2)
         (convToController Conv2 P2)
         (:not-equal linker P2)
         (peToER P2 PR2)
         (:not-equal PR2 PR1)
         ))))

(defn pattern2c []
  (defpattern Pattern2c [PR1 :ProteinReference, PR2 :ProteinReference]
    "A link from protein reference %s to anther protein reference %s."
    (erToPE PR1 P1)
    (peToControlledConv P1 Conv1)
    ((Participant. RelType/OUTPUT) Conv1 linker)
    (:not-equal linker P1)
    ((ParticipatesInConv. RelType/INPUT) linker Conv2)
    (:not-equal Conv1 Conv2)
    (convToController Conv2 P2)
    (:not-equal linker P2)
    (peToER P2 PR2)
    (:not-equal PR2 PR1)
    ))

(defn pattern2d []
  (defpattern Pattern2D [?PR1 :ProteinReference, ?PR2 :ProteinReference]
    "A link from protein reference %s to anther protein reference %s."
    (:referenced-protein ?PR1 ?P1)
    (:controlled-by ?P1 ?Conv1)
    (:new-output-node ?Conv1 ?linker)
    (:not-equal ?linker ?P1)
    (:new-input-node ?linker ?Conv2)
    (:not-equal ?Conv1 ?Conv2)
    (:controller-for ?Conv2 ?P2)
    (:not-equal ?linker ?P2)
    (:protein-reference-for ?P2 ?PR2)
    (:not-equal ?PR2 ?PR1)
    ))

(defn pattern3 []
  (defpattern Pattern3 [Conv :Conversion, ER :EntityReference]
    "A link from conversion %s to entity reference %s."
    (left   Conv    leftPE )
    (peToER leftPE  ER     )
    (right  Conv    rightPE)
    (peToER rightPE ER     )
    ))

(defn pattern4a [] 
  (defpattern Pattern4 [Conv :Conversion, leftPE :PhysicalEntity]
    "The left side of conversion %s is %s."
    (left Conv leftPE))) 

(deftest test-make-patterns 
  (testing "Test just making a few patterns."
    (let [xpattern? (private-function xpattern? drae.paxll)]
      (is (xpattern? (pattern2d)))
      (is (xpattern? (pattern3)))
      (is (xpattern? (pattern4a)))
      )))

(defn search4 []
  (Searcher/search (model :raf-cascade) (pattern4a)))

(def test-proteins 
  '(ATXN3 LRRC15 PKP1 KRT85 KRT84 KRT31 KRT34 CALML3 SFN RPL34 SERPINB5 PRDX6 NoSymbol 
          SPINK5 DSG4 KRT33B OTUB2 TGM3 KRT15 POF1B CRYAB LGALS7 KRT16 MYH14 GSN 
          SHMT2 CKB BRE KRT82 ANXA1 HSPA2 ACTN1 GTF3C1 SERPINB4))

(defn check-test-proteins
  ([protein-names model]
    (pmap (fn [protein-name]
            (vector protein-name (find-protein-references protein-name model)))
          protein-names))
  ([protein-names]
    (check-test-proteins protein-names (model))))


;;; TEST DATA
;;; -----------------------------------------------------------------------------------

(def sowa-all 
  '(ATXN3 LRRC15 PKP1 KRT85 KRT84 KRT31 KRT34 CALML3 SFN RPL34 SERPINB5 PRDX6 NoSymbol 
   SPINK5 DSG4 KRT33B OTUB2 TGM3 KRT15 POF1B CRYAB LGALS7 KRT16 MYH14 GSN SHMT2 CKB 
   BRE KRT82 ANXA1 HSPA2 ACTN1 GTF3C1 SERPINB4 JUP KRT14 LAP3 KIAA0157 GSTP1 
   MDH2 LOC347701 LGALS3 DSG1 ANXA2P2 DSP GNL3 EIF5B PA2G4 GPIAP1 G3BP1 ALB 
   EIF2S3 S100A7 RPL3 EIF2S1 DHX9 RPS15 SERBP1 RPL23 KRT6B KRT5 RPL18A KRT1 RPS19 
   PTCD3 KRT38 KRT36 RPS9 NCL KRT4 RPS4X USP10 PARP1 GNB2L1 GLG1 RPLP0 HSPA8 HSPA5 
   FXR1 RPS3 DHX30 RPL4 KRT75 RPS17 FMR1 BXDC2 FXR2 KRT10 VCP LOC149224 NAT10 KRT9 KRT6A 
   ENO1 KRT3 RPS16 RPS6 RPS10 RPL26L1 HSPB1 RPL23A TSR1 LOC137107 G3BP2 RPL13A RPL35 
   DDX47 LOC641856 PLEC1 RPL7 KRT81 IMP4 LCN2 RPS2 GDI1 LOC497661 ILF2 DSC3 RPL9 DIMT1L 
   OSBPL8 RPLP2 NSUN2 PTBP1 LUC7L2 SRP14 RPL6 RPL21 ACTA2 SRPK1 TOP1 RPL22L1 SRP68 TUBB2C 
   RPS15A RPS13 RPS8 NPM1 RPS24 IGF2BP3 RPL27 AMOTL2 FTSJ3 LOC388344 RPL7A DNAJB1 SNRPD2 
   HSPA9 STAU1 USP11 C12orf31 tcag7.350 RPL28 RPS29 CCDC124 MAGEB2 EIF2S2 BAT2D1 RPL18 
   RPS7 CALML5 RPL10A HNRPM SIAHBP1 RPS18 LOC284393 HP1BP3 IGF2BP2 EIF4A1 NOL1 KRT13 
   ILF3 HNRPA2B1 RPL36AL KRT2 MKI67 HSPA1B EEF2 RPL8 MOV10 RPL36 LOC389901 ATP5B RPL15 
   KRT17 HSPA1L RPS14 C1orf25 CTSD RPS21 SND1 KRT76 HSD17B10 RPL14 NACA DDX24 LOC136143 
   FAM111A TUBB2A HSPC111 ERLIN1 HIST1H2BL HNRPR LUC7L AP3D1 HNRPU NPW HIST2H2BE MYH9 MYL6 
   TRIM25 PSMA5 EIF5A2 CST3 DHX15 KRT20 SSRP1 DSTN ROD1 RPL30 PTBP2 TFAM RPL5 RPL10L RPS5 
   CLTCL1 RPL12 RPL17 SYNCRIP HIST1H4I RPL38 BAG2 RPS25 ACTB DDX21 RPL19 LYZ CLTC RPS12 
   GAPDH TUBA4A XRCC5 GTPBP4 RPS3A ANXA2 HERC5 PRDX1 H1F0 NFKB1 TCOF1 C11orf48 PABPC1 SFRS1 
   ENO1B FAU S100A8 EIF2AK2 FASN DDX5 KRT6C HIST1H1C RPLP1 SLC25A5 DNAJC9 RPL31 hCG_1984468 
   ELAVL1 TBL2 SFRS11 HARS2 HSPA4L DNAPTP6 LGALS3BP SRRM2 ATP5A1 SSB HNRPC PTRF MATR3 
   XRCC6 MTDH STRBP GRWD1 KRT19 RPS20 SNRPD1 CSDA TUBB RBM28 SUB1 RDX RPS27A RSL1D1 RPS23 
   RPL37A RFC1 PURA RPL13 HIST1H2AH SNRPD3 HNRPD HIST1H2AE RPL22 KRT80 HSPC142 CFL1 PRDX3 
   POP7 ALDOA LOC285176 CICK0721Q.1 LMNA EEF1G EEF1D SCYE1 AP2A2 LOC390876 HIST1H2AA GTPBP9 
   RPL26 POLR1C WDR12 SF3B14 SFRS6 C1QBP LRRC59 DDX18 HSPH1 EEF1A2 RPL24 hCG_21078 HNRPK 
   H1FX SFRS3 FAM120A EEF1A1 SRP72 DDX54 ASPH S100A9 CCT6A U2AF2 DGAT2L7 HNRPCL1 DKFZp686D0972 
   YBX1 SF3B3 IGF2BP1 LYAR LOC650788 RPL11 HSPA4 POLRMT TXN C14orf166 ENO2 SHMT1 SNRPB PPIA 
   GPATCH4 ATAD3A HNRPF SRRM1 GNL2 SLC25A3 TUFM CTSB THOC4 HNRPA1 MIF SFRS12 GFAP CGI-09 CCT3 
   TUBA1C ABCF1 LOC729708 TUBB1 HNRPA0 AMY1A HSPA7 SFRS10 HSP90AA1 HSP90AA2 NAP1L1 RUVBL1 
   LOC152663 LIG3 HSP90B1 PPP2R2B MRPL12 RFC5 HSP90AB1 VIL2 KRT24 HNRPH1 NCBP1 LOC652595 SDHA 
   DAP3 UBAP2L WDR5 KRT8 SSR1 SRP9 HNRPDL KPNA2 SART3 ADAR DDX1 SNRP70 TRAP1 LOC729611 BRCC3 
   CCDC98 UIMC1 IMMT HIST2H3C BBX LLGL2 SPATA5 SGPL1 IGHMBP2 SUPT16H SF3B1 PRPF8 PRKDC NARG1 
   MINA RUVBL2 ZNF622 POLR1A TRIM56 GTF3C4 PUS7 DHX29 UBTF FLJ11184 PYCR1 ABCF2 AP2B1 FARSB 
   TUBB6 NMT1 RBM34 RPSA RPP14 NUFIP2 PRPF38B DRG1 DYNC1H1 SRPK2 EFTUD2 DDX55 MCM7 NOP5/NOP58 
   HMGA1 PPIB BTF3 TUBB4 CCT5 HDLBP RPS11 PDCD11 PRKRA DNAJA2 RPP30 MYBBP1A RRBP1 EXOSC10 FBL 
   FKBP3 MRPL40 SFRS5 DNAJA1 XRN2 POP1 AP2A1 TROVE2 SART1 PSMD12 PCBP1 FUSIP1 HSPD1 GTPBP10 
   RAD54L2 hCG_1790262 PSMC5 METAP1 SAMHD1 CCBL2 FARSA LARP1 SSR4 LOC387867 
   NOL5A ITGB4BP TUBB3 PRPF19 NACAP1 DDX17 DDX52 CAD PABPC4 RBM39 USP39 NOLA1 GCS1 RBM35A 
   MGC16597 PAPD1 SDCCAG1 SUCLG2 AP1M2 USP22 IDH3B YTHDC2 CDC5L RPSAP15 NMT2 PPP1CC RPA1 RCN2 
   RFC4 AMPD1 SFRS2 EPRS HNRPAB PARP2 LARP7 EDF1 LRRC47 APOBEC3G RBM10 DDX50 PRPF40A MRPS9 
   PSMC4 UPF1 DDX3X POLR1E CD3EAP SLC25A6 CLTA SPATS2 PSMC2 PCNA TCP1 hCG_2023776 MAP7D1 
   FLJ12529 PWP2 C1orf35 LARP4 BAG5 DEK ASCC3 TRPT1 FAM60A COPB1 AP4E1 C10orf47 RPL35A HMGB2 
   MYO1F DHX37 EMG1 KIAA0179 RBBP6 EIF3S10 FAM8A1 STK39 DPM1 FBXO33 PCBP2 DNAJB4 PPM2C C17orf79 
   RPN2 PNPLA6 MRM1 SCD CBWD2 MRPS23 CUGBP1 SLC2A9 TSPYL1 MKI67IP BOP1 STUB1 SUCLG1 FLJ12949 
   PSIP1 ALDH1B1 NSUN5 WDR18 HNRPUL1 CPSF6 CIRBP U2AF1 DNAJA3 EXOSC2 RPS27L EBNA1BP2 RFC2 
   ATP1A2 MRPS5 PSMC3 WIBG GART C8orf33 DKC1 MRTO4 ATP5C1 KRT18 STK10 SEC61A1 NOM1 ZCCHC3 NPM3 
   CLK3 POLR1B SSR3 MRPS15 GTF2F1 MSH6 ABCD3 DYNC1LI1 CSNK1A1L RARS ASCC3L1 EIF2C2 SNRPC 
   C22orf28 DDB1 SNRPG MRPS27 PSMC1 LOC345630 AYTL2 RRP12 LSM12 EXOSC7 BANF1 HABP4 RRP9 ATP2A1 
   AHSA1 GPSN2 EXOSC9 RBM3 ILVBL CTPS PSMD2 TRA2A APEX1 SNRPE CCT7 SF3B4 GLTSCR2 THEX1 SSBP1 
   MCM3 HNRPH2 HNRNPG-T CCT2 SKP1A FAM98A CROP HNRPH3 TRIM28 HNRPA3 FUS COPS4 COPS2 COPS5 CUL4B 
   COPS3 GPS1 CUL4A CUL1 DDB2 COPS7A COPS6 COPS8 COPS7B BTBD2 KLHL8 CUL2 VPRBP WDR23 KLHDC5 
   CUL3 BTBD1 FBXO17 WDR21A KLHL18 NEDD8 ERCC8 NKX1-1 TFG FBXO11 TNRC6B MVP TCEB1 SPTAN1 LOC401072 
   TAF15 TIAL1 DNAJC10 EWSR1 TRIM21 ENTPD1 KARS FBXO44 NONO KLHL12 HNRPL RCBTB1 APPBP2 BAG4 
   TAOK2 FBXO7 IL17RE LOC644540 NUP210 SPTBN1 RNMT SMARCB1 CSTB PLOD1 KRT71 DAZAP1 RBM14 DPF2 
   SMARCC2 PDXK NKRF TOP2A ETF1 GRSF1 DDX10 MGC10433 PABPN1 HMG1L10 IFRD2 SFRS4 NAP1L4 KRR1 DHX57 
   METAP2 ZFR WDR61 ACAD11 PLRG1 SNRPA RBM25 DDX46 EIF1AY SNRPB2 RG9MTD1 C9orf84 SENP3 HNRPUL2 
   EPB41L5 UTP18 KIF14 LOC652708 TLR2 FLII AHNAK SLC35B2 PPP1R15A TFB1M LOC388621 CYBA LTV1 LRP10 
   DRAP1 TRPM6 EIF4EBP3 RRS1 PWP1 SNRPF C3orf17 NDUFAB1 SFRS7 FRG1 PPIH PPAN EIF4A3 IARS TBL3 
   hCG_15200 PRMT1 LOC440733 UCHL1 USP20 IBTK GDF2 TRMT1 EXOSC6 BMP3 SFPQ ZC3H15 MRPS22 MRPL46 PIAS3 
   NOC3L PARP14 KRT78 C14orf172 SRPK3 LOC124220 TRIP12 MYCBP2 PKN2 ARHGAP29 RAE1 KEAP1 SPRYD3 CS 
   USP7 MCCC2 MPHOSPH9 GARS WRNIP1 OSBPL10 EIF4B PGAM5 CLPX FBXO45 BUB3 POLR2E OSBPL9 HOOK2 C22orf9 
   MCM5 POLR2H DNAJC7 TARDBP C4orf15 ZNF276 
   C14orf94 FER1L3 TIMM50 FKBP8 PPP2R2A UGDH POLR1D FAM29A AAMP CEP27 SLC16A1 NY-SAR-48 ATAD3B TKT 
   NUP153 MYO6 USP4 DLD ATP6V1G2 TCEAL4 ACSL4 CCT4 ATP1A1 RAD17 KDELR2 KIAA0841 KPNB1 RPN1 CCT8 ACSL5 
   UGCGL1 CDC2 ALAS1 FAT PKM2 PFKP ZNF295 ATP2A2 PARN USP43 PKP3 HLTF TMEM33 USP15 PSMC6 RDBP 
   IVNS1ABP ELP3 SSFA2 DGCR8 YWHAQ HAT1 GNL3L CEP250 NDUFA10 PRDX2 GCLM LGALS1 KIAA0515 ADRBK1 
   JOSD2 PSMA7 SLC25A22 MTHFD2 MAP4 GCAT MLL3 BSG EPHA2 PSMD1 P4HA1 CPOX EIF3S9 C9orf93 USP54 
   BAG3 CCDC5 PRMT5 CNP FLYWCH1 FAM98B TK1 CREG1 TMEM104 HSPA6 SLC25A11 LOC388720 SEC61A2 MTHFD1L 
   PPP1CB GTF3C5 TUBA4B PDHB MTHFD1 PHLDA2 CEP135 NUBP2 RTKN MAT2A DHX40 SERPINH1 PKLR POLE2 
   YWHAZ KHSRP CANX RFC3 SLC25A4 TUBA1A HLA-B ACADM WDR68 HLA-H AHCY DDX6 SRPRB EIF3S5 SLC3A2 
   RBBP4 DARS DCD HERC2 VAPA KIAA1787 VAPB EIF3S8 PLEKHA7 N4BP3 MTUS1 EIF3S6 OAT CTNND1 SYTL4 
   FECH KIAA0310 TFB2M ZFPL1 PLK1 EIF3S7 TRIM32 UNC84B AAAS ATP6AP1 PTDSS1 ATP6AP2 DNM2 LMNB1 
   POM121 MAP3K4 MOSPD1 HERC2P2 STT3B ACAD9 MOSPD2 STAT3 C14orf21 PARS2 SLC2A1 WARS DNAJC16 TMPO 
   CALM1 CKAP4 CARM1 UTP15 USP9X POLDIP3 PHGDH CTNNA1 NUP155 ZNF770 NUP160 CBX4 TRRAP CBARA1 C9orf167 
   APBB2 PATZ1 SPTLC2 TMEM48 UBE3C RNF2 GPT2 ERAL1 CSE1L USP33 RAB11FIP5 TUT1 MLSTD2 EMD ITGB4 
   TNRC6A ECH1 GNPAT ZNF638 CEPT1 ATP6V0C TMEM55B RGPD5 PDCD6IP PHB2 CMAS ITGA1 PDCD2 RAB8A ZDHHC5 
   CCDC47 STRAP PBEF1 SRPR MAPK6 TOR1AIP1 KCND3 ARMC6 PAICS RAB5C ALDH1A3 LOC728774 JPH1 TUBG1 SET 
   PARK7 DDX28 YME1L1 OLFML2A TAF6L LRPPRC LARP5 SELS KIAA0391 CDK3 EFHA1 NUP50 LOC647000 FAM105A 
   COPA GTF2H3 FAM3C EXDL2 APOBEC3D IGKV4-1 YWHAE SUPT3H HADHA TAF5L DNAJB11 USP21 MARK2 MARK3 PPP2CA 
   TTN PFN1 ARG1 DSC1 RP1-14N1.3 ODZ3 DKFZp434O0320 MARK4 DMTF1 EXOSC5 FAM83H AHI1 C6orf148 PHKG2 POLK 
   KIAA1012 LDHB KRT73 IGKV1-5 AZGP1 PIGR CSTA YWHAG H2AFV IGHA2 SIRT1 LTF ATXN7L3 ATXN7 IGHA1 
   SERPINA1 TF SERPINB3 LCP1 ENY2 C3 PIP MMP9 ATXN7L2 IGHM HP BPIL1 F7 ANXA3 TAF9 TCN1 MPO C6orf58 
   FAM48A SERPINB1 B2M TADA1L CASP14 IGJ USP51 SERPINA3 AASDHPPT LPO TTR IGKV3-20 USP27X DMBT1 
   SERPINB13 LOC552889 STATH GPI PRH1 SUPT7L ANXA5 CAT SERPINB12 AMY2B QSCN6 GBE1 TPI1 MSN SCGB1D2 
   TAGLN2 CCDC101 PYGL SOD2 PGD TMED2 FABP3 C20orf114 PDIA6 TAF10 TALDO1 PSMB3 HSPE1 VTN TACSTD1 
   KRT77 PLUNC PGAM2 ORM2 CAPG XDH TADA3L LDHA MGC21874 LOC651278 PCBD1 USP28 TP53BP1 TNRC6C AGL 
   PHB SMARCA4 EIF2C1 QDPR SMARCC1 NUDT1 DLAT ARID1A CPSF1 TPR SF1 TTBK2 QARS RBM7 IQGAP1 ZCCHC11 
   FBXW8 KRT6L SEC13 VSIG8 LANCL1 ANXA7 SEC23A RRP15 KPNA6 LARS CACNA1I HDAC1 LY9 SELENBP1 NCBP2 
   EIF2C3 IFIT5 PALLD NUDT16L1 ACTL6A EEF1B2 USP3 RPL3L BXDC1 SUPT5H DDX27 ZC3HAV1 PRPF4B COIL 
   AKAP8L AATF LOC339977 GM2A PUS1 RPP38 KATNA1 LOC130773 PRPF3 PRPF31 PRPF4 LSM4 BCDIN3 C17orf68 
   LSM8 LSM2 LSM6 NHP2L1 FBXO3 ADSL SEP15 DOCK1 POLDIP2 SLC22A6 LSM7 LSM3 EDC3 AKAP7 USP32 ANAPC1 
   JARID1B PRPF39 SRPX CROCC SF3B2 FLNA SMARCE1 TJP1 CORO1C NAV2 SEC24B NQO2 SEC24A MAPK13 EEF1E1 GTF3C3))

(def sowa10 "Ten protein names from Sowa 2009." (take 10 sowa-all))

(def sowa100 "One hundred protein names from Sowa 2009" (take 100 sowa-all))

(def sowa-set "All Sowa proteins as a sorted set." (into (sorted-set) (map str sowa-all)))

;;; DEAD CODE

;;; Tutorial Examples
;;; -------------------------------------------------------------------------
#_(defn basics1 [] (new-model "http://biopax.org/tutorial/"))

#_(defn basics2 [model] 
   (let [p (. model (addNew (bioentity-class :Protein) "MyProtein"))]
     (doto p
       (.addName "Tutorial Example Some Transporter 1")
       (.setDisplayName "TEST1"))))

#_(defn basics2a [model]
   (add-protein model 
                :fullname "Tutorial Example Some Transporter 1"
                :display-name "TEST1"))

#_(defn check-test-proteins-old 
   ([protein-names model]
     (for [protein-name protein-names]
       (vector protein-name (find-protein-references protein-name model))))
   ([protein-names] (check-test-proteins-old protein-names (model))))

