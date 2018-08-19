;;; (setq *maxima-dir* "path/maxima-5.X.Y") will be added before here.
(if (not (probe-file *maxima-dir*)) (quit))
#|
/*
    Copyright 2012, 2013 Yasuaki Honda (yasuaki.honda@gmail.com)
    This file is part of MaximaOnAndroid.

    MaximaOnAndroid is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    MaximaOnAndroid is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MaximaOnAndroid.  If not, see <http://www.gnu.org/licenses/>.
*/
|#

;;; set up maxima_tempdir
(ensure-directories-exist "/data/data/jp.yhonda/files/")

(defun maxima::getpid () (si::getpid))
(defun maxima-getenv (a)
    (cond ((string-equal a "MAXIMA_PREFIX") *maxima-dir*)
          ((string-equal a "MAXIMA_TEMPDIR") "/data/data/jp.yhonda/files/")))
(setq *maxima-default-layout-autotools* "false")
(setq *autoconf-prefix* *maxima-dir*)
(setq *maxima-source-root* *maxima-dir*)
(setq *maxima-prefix* *maxima-dir*)
(set-pathnames)                     
(setq *prompt-suffix* (format nil "~A" (code-char 4)))

;;; describe(cmd) is not supported
(defmspec $describe (x) t)

;;; Support for declare(f,antisymmetric)
(defmfun antisym (e z)
  (when (and $dotscrules (mnctimesp e))
    (let ($dotexptsimp)
      (setq e (simpnct e 1 nil))))
  (if ($atom e) e (antisym1 e z)))

(defun antisym1 (e z)
  (let ((antisym-sign nil)
	(l (mapcar #'(lambda (q) (simpcheck q z)) (cdr e))))
    (when (or (not (eq (caar e) 'mnctimes)) (freel l 'mnctimes))
      (multiple-value-setq (l antisym-sign) (bbsort1 l)))
    (cond ((equal l 0) 0)
	  ((prog1
	       (null antisym-sign)
	     (setq e (oper-apply (cons (car e) l) t)))
	   e)
	  (t (neg e)))))

;;; Support for zn_log()
(defmfun $zn_log (a g n &optional fs-phi)
  (unless (and (integerp a) (integerp g) (integerp n))
    (return-from $zn_log
      (if fs-phi
        (list '($zn_log) a g n fs-phi)
        (list '($zn_log) a g n) )))
  (when (minusp a) (setq a (mod a n)))
  (cond
    ((or (= 0 a) (>= a n)) nil)
    ((= 1 a) 0)
    ((= g a) 1)
    ((> (gcd a n) 1) nil)
    (t
      (if fs-phi
        (if (and ($listp fs-phi) ($listp (cadr fs-phi)))
          (progn
            (setq fs-phi (mapcar #'cdr (cdr fs-phi))) ; Lispify fs-phi
            (setq fs-phi (cons (totient-from-factors fs-phi) fs-phi)) )
          (gf-merror (intl:gettext
             "Fourth argument to `zn_log' must be of the form [[p1, e1], ..., [pk, ek]]." )))
        (setq fs-phi (totient-with-factors n)) )
      (cond
        ((not (zn-primroot-p g n
                             (car fs-phi)                   ;; phi
                             (mapcar #'car (cdr fs-phi)) )) ;; factors without multiplicity
          (gf-merror (intl:gettext
            "Second argument to `zn_log' must be a generator of (Z/~MZ)*." ) n ))
        ((= 0 (mod (- a (* g g)) n))
          2 )
        ((= 1 (mod (* a g) n))
          (mod -1 (car fs-phi)) )
        (t
          (zn-dlog a g n
                   (car fs-phi)         ;; phi
                   (cdr fs-phi) ) ))))) ;; factors with multiplicity

(defun zn-dlog (a g n ord fs-ord)
  (let (p e ord/p om xp xk mods dlogs (g-inv (inv-mod g n)))
    (dolist (f fs-ord)
      (setq p (car f) e (cadr f)
            ord/p (truncate ord p)
            om (power-mod g ord/p n) ;; om is a generator of prime order p
            xp 0 )
      ;; Let op = ord/p^e, gp = g^op (mod n), ap = a^op (mod n) and
      ;;     xp = x (mod p^e).
      ;; gp is of order p^e and therefore
      ;;   (*) gp^xp = ap (mod n).
      (do ((b a) (k 0) (pk 1) (acc g-inv) (e1 (1- e))) (()) ;; Solve (*) by solving e logs ..
        (setq xk (dlog-rho (power-mod b ord/p n) om p n))   ;;   .. in subgroups of order p.
        (incf xp (* xk pk))
        (incf k)
        (when (= k e) (return)) ;; => xp = x_0+x_1*p+x_2*p^2+...+x_{e-1}*p^{e-1} < p^e
        (setq ord/p (truncate ord/p p)
              pk (* pk p) )
        (when (/= xk 0) (setq b (mod (* b (power-mod acc xk n)) n)))
        (when (/= k e1) (setq acc (power-mod acc p n))) )
      (push (expt p e) mods)
      (push xp dlogs) )
    (car (chinese dlogs mods)) )) ;; Find x (mod ord) with x = xp (mod p^e) for all p,e.

;; baby-steps-giant-steps:

(defun dlog-baby-giant (a g p n) ;; g is generator of order p mod n
  (let* ((m (1+ (isqrt p)))
         (s (floor (* 1.3 m)))
         (gi (inv-mod g n))
          g^m babies )
    (setf babies
      (make-hash-table :size s :test #'eql :rehash-threshold 0.9) )
    (do ((r 0) (b a))
        (())
      (when (= 1 b)
        (clrhash babies)
        (return-from dlog-baby-giant r) )
      (setf (gethash b babies) r)
      (incf r)
      (when (= r m) (return))
      (setq b (mod (* gi b) n)) )
    (setq g^m (power-mod g m n))
    (do ((rr 0 (1+ rr))
         (bb 1 (mod (* g^m bb) n))
          r ) (())
      (when (setq r (gethash bb babies))
        (clrhash babies)
        (return (+ (* rr m) r)) )) ))

;; brute-force:

(defun dlog-naive (a g n)
  (do ((i 0 (1+ i)) (gi 1 (mod (* gi g) n)))
      ((= gi a) i) ))

;; Pollard rho for dlog computation (Brents variant of collision detection)

(defun dlog-rho (a g p n) ;; g is generator of prime order p mod n
  (cond
    ((= 1 a) 0)
    ((= g a) 1)
    ((= 0 (mod (- a (* g g)) n)) 2)
    ((= 1 (mod (* a g) n)) (1- p))
    ((< p 512) (dlog-naive a g n))
    ((< p 65536) (dlog-baby-giant a g p n))
    (t
      (prog ((b 1) (y 0) (z 0)    ;; b = g^y * a^z
             (bb 1) (yy 0) (zz 0) ;; bb = g^yy * a^zz
             dy dz )
        rho
        (do ((i 0)(j 1)) (()) (declare (fixnum i j))
          (multiple-value-setq (b y z) (dlog-f b y z a g p n))
          (when (equal b bb) (return))                 ;; g^y * a^z = g^yy * a^zz
          (incf i)
          (when (= i j)
            (setq j (1+ (ash j 1)))
            (setq bb b yy y zz z) ))
        (setq dy (mod (- y yy) p) dz (mod (- zz z) p)) ;; g^dy = a^dz = g^(x*dz)
        (when (= 1 (gcd dz p))
          (return (zn-quo dy dz p)) ) ;; x = dy/dz mod p (since g is generator of order p)
        (setq y 0
              z 0
              b 1
              yy (1+ (random (1- p)))
              zz (1+ (random (1- p)))
              bb (mod (* (power-mod g yy n) (power-mod a zz n)) n) )
        (go rho) ))))

;; iteration for Pollard rho:  b = g^y * a^z in each step

(defun dlog-f (b y z a g ord n)
  (let ((m (mod b 3)))
    (cond
      ((= 0 m)
        (values (mod (* b b) n) (mod (ash y 1) ord) (mod (ash z 1) ord)) )
      ((= 1 m) ;; avoid stationary case b=1 => b*b=1
        (values (mod (* a b) n) y                   (mod (+ z 1) ord)  ) )
      (t
        (values (mod (* g b) n) (mod (+ y 1) ord)   z                ) ) )))

;;; Galois Field support
(defmfun $gf_index (a)
  (gf-data? "gf_index")
  (gf-log-errchk1 *gf-prim* "gf_index")
  (let ((*ef-arith?*))
    (if (= 1 *gf-exp*)
      ($zn_log a (gf-x2n *gf-prim*) *gf-char*)
      (gf-dlog (gf-p2x a)) )))

(defmfun $gf_log (a &optional b)
  (gf-data? "gf_log")
  (gf-log-errchk1 *gf-prim* "gf_log")
  (let ((*ef-arith?*))
    (cond
      ((= 1 *gf-exp*)
        ($zn_log a (if b b (gf-x2n *gf-prim*)) *gf-char*) ) ;; $zn_log checks if b is primitive
      (t
        (setq a (gf-p2x a))
        (and b (setq b (gf-p2x b)) (gf-log-errchk2 b #'gf-prim-p "gf_log"))
        (if b
          (gf-dlogb a b)
          (gf-dlog a) )))))

(defun tex-char (x) 
  (cond ((equal x #\ ) "\\space ")
        ((equal x #\_) "\\_ ")
        (t x)))

(defprop mlessp ("\\lt ") texsym)
(defprop mgreaterp ("\\gt ") texsym)

(defun tex-string (x)
  (cond ((equal x "") "")
	((eql (elt x 0) #\\) x)
	(t (concatenate 'string "\\text{" x "}"))))

;;; Don't know why, but fib(n) returns 0 regardless n value.
;;; The followings fix this.
(defmfun $fib (n)
  (cond ((fixnump n) (ffib n))
    (t (setq $prevfib `(($fib) ,(add2* n -1)))
       `(($fib) ,n))))

(defun ffib (%n)
  (declare (fixnum %n))
  (cond ((= %n -1)
     (setq $prevfib -1)
     1)
    ((zerop %n)
     (setq $prevfib 1)
     0)
    (t
     (let* ((f2 (ffib (ash (logandc2 %n 1) -1))) ; f2 = fib(n/2) or fib((n-1)/2)
        (x (+ f2 $prevfib))
        (y (* $prevfib $prevfib))
        (z (* f2 f2)))
       (setq f2 (- (* x x) y)
         $prevfib (+ y z))
       (when (oddp %n)
         (psetq $prevfib f2
            f2 (+ f2 $prevfib)))
       f2))))

;;; Dropbox support
(let ((top (pop $file_search_maxima))) 
    (push "/sdcard/Download/$$$.txt" $file_search_maxima) 
    (push top $file_search_maxima))
(let ((top (pop $file_type_maxima))) 
    (push "txt" $file_type_maxima) 
    (push top $file_type_maxima))

;;; qepcad support
(defun $system (&rest args)
  (let ((bashline "bash -c 'export qe="))
    (if (>= (string> (first args) bashline) 
            (length bashline))
      ;; perform qepcad
      (progn
        (format t "start qepcad~A" *prompt-suffix*)
        (read-line)))))

(let ((top (pop $file_search_lisp))) 
    (push "/data/data/jp.yhonda/files/additions/qepcad/qepmax/$$$.{lsp,lisp,fasl}" $file_search_lisp) 
    (push top $file_search_lisp))
(let ((top (pop $file_search_maxima))) 
    (push "/data/data/jp.yhonda/files/additions/qepcad/qepmax/$$$.{mac,mc}" $file_search_maxima) 
    (push top $file_search_maxima))


(progn                                                                      
  (if (not (boundp '$qepcad_installed_dir))                                 
      (add2lnc '$qepcad_installed_dir $values))                             
  (defparameter $qepcad_installed_dir                                              
                "/data/data/jp.yhonda/files/additions/qepcad")
  (if (not (boundp '$qepcad_input_file))                                 
      (add2lnc '$qepcad_input_file $values))                             
  (defparameter $qepcad_input_file                                              
                "/data/data/jp.yhonda/files/qepcad_input.txt")           
  (if (not (boundp '$qepcad_output_file))                                       
      (add2lnc '$qepcad_output_file $values))                                   
  (defparameter $qepcad_output_file                                             
                "/data/data/jp.yhonda/files/qepcad_output.txt")
  (if (not (boundp '$qepcad_file_pattern))                                       
      (add2lnc '$qepcad_file_pattern $values))                                   
  (defparameter $qepcad_file_pattern "/data/data/jp.yhonda/files/qepcad*.txt")
  (if (not (boundp '$qepcad_option))                                       
      (add2lnc '$qepcad_option $values))                                   
  (defparameter $qepcad_option " +N20000000 +L100000 "))

;;; always save support
(defvar *save_file* "/data/data/jp.yhonda/files/saveddata")
(defun $ssave () (meval `(($save) ,*save_file* $labels ((mequal) $linenum $linenum))) t)
(defun $srestore () (load *save_file*) t)

(setq $in_netmath nil)

($set_plot_option '((mlist) $plot_format $gnuplot))
($set_plot_option '((mlist) $gnuplot_term $canvas))
($set_plot_option '((mlist) $gnuplot_out_file "/data/data/jp.yhonda/files/maxout.html"))
(setq $draw_graph_terminal '$canvas)
  
;;; displa support
($load '$stringproc)
(setq $display2d '$imaxima)
(let ((old-displa (symbol-function 'maxima::displa)))
  (declare (special maxima::$display2d))
  (defun maxima::displa (form) 
    (if (eql maxima::$display2d 'maxima::$imaxima)
	(if (and (equal (car form) '(mlabel)) (not (null (second form))))
	    (format t "$$$$$$ RO1 ~A ~A $$$$$$" (second form) (maxima::$tex1 (third form)))
	   (format t "$$$$$$ RO2 ~A $$$$$$" (maxima::$tex1 form)))
      (funcall old-displa form))))

($load '$draw)

($set_draw_defaults                                                             
   '((mequal simp) $terminal $canvas)                                           
   '((mequal simp) $file_name "/data/data/jp.yhonda/files/maxout"))             

;;; /data/local/tmp/maxima-init.mac
(setq $file_search_maxima                                                  
        ($append '((mlist) "/data/local/tmp/###.{mac,mc}")                  
                 $file_search_maxima))                               
(if (probe-file "/data/local/tmp/maxima-init.mac") ($load "/data/local/tmp/maxima-init.mac"))

;;; lisp-utils/defsystem.lisp must be loaded.
($load "lisp-utils/defsystem")


;;; some functions in matrun.lisp does not work. They are redefined here.
;;; It is just like fib above.

(defmspec $apply1 (l) (setq l (cdr l))
	  (let ((expr (meval (car l))))
	    (mapc #'(lambda (z) (setq expr (apply1 expr z 0))) (cdr l))
	    expr))

(defmfun apply1 (expr *rule depth) 
  (cond
    ((> depth $maxapplydepth) expr)
    (t
     (prog nil 
	(*rulechk *rule)
	(setq expr (rule-apply *rule expr))
	b    (cond
	       ((or (atom expr) (mnump expr)) (return expr))
	       ((eq (caar expr) 'mrat)
		(setq expr (ratdisrep expr)) (go b))
	       (t
		(return
		  (simplifya
		   (cons
		    (delsimp (car expr))
		    (mapcar #'(lambda (z) (apply1 z *rule (1+ depth)))
			    (cdr expr)))
		   t))))))))

(defmspec $applyb1 (l)  (setq l (cdr l))
	  (let ((expr (meval (car l))))
	    (mapc #'(lambda (z) (setq expr (car (apply1hack expr z)))) (cdr l))
	    expr))

(defmfun apply1hack (expr *rule) 
  (prog (pairs max) 
     (*rulechk *rule)
     (setq max 0)
     b    (cond
	    ((atom expr) (return (cons (multiple-value-bind (ans rule-hit) (mcall *rule expr) (if rule-hit ans expr)) 0)))
	    ((specrepp expr) (setq expr (specdisrep expr)) (go b)))
     (setq pairs (mapcar #'(lambda (z) (apply1hack z *rule))
			 (cdr expr)))
     (setq max 0)
     (mapc #'(lambda (l) (setq max (max max (cdr l)))) pairs)
     (setq expr (simplifya (cons (delsimp (car expr))
				 (mapcar #'car pairs))
			   t))
     (cond ((= max $maxapplyheight) (return (cons expr max))))
     (setq expr (rule-apply *rule expr))
     (return (cons expr (1+ max)))))

(defun rule-apply (*rule expr)
  (prog (ans rule-hit)
   loop (multiple-value-setq (ans rule-hit) (mcall *rule expr))
   (cond ((and rule-hit (not (alike1 ans expr)))
	  (setq expr ans) (go loop)))
   (return expr)))

(defmspec $apply2 (l) (setq l (cdr l))
	  (let ((rulelist (cdr l))) (apply2 rulelist (meval (car l)) 0)))

(defmfun apply2 (rulelist expr depth) 
  (cond
    ((> depth $maxapplydepth) expr)
    (t
     (prog (ans ruleptr rule-hit) 
      a    (setq ruleptr rulelist)
      b    (cond
	     ((null ruleptr)
	      (cond
		((atom expr) (return expr))
		((eq (caar expr) 'mrat)
		 (setq expr (ratdisrep expr)) (go b))
		(t
		 (return
		   (simplifya
		    (cons
		     (delsimp (car expr))
		     (mapcar #'(lambda (z) (apply2 rulelist z (1+ depth)))
			     (cdr expr)))
		    t))))))
      (cond ((progn (multiple-value-setq (ans rule-hit) (mcall (car ruleptr) expr)) rule-hit)
	     (setq expr ans)
	     (go a))
	    (t (setq ruleptr (cdr ruleptr)) (go b)))))))

(defmspec $applyb2 (l) (setq l (cdr l))
	  (let ((rulelist (cdr l))) (car (apply2hack rulelist (meval (car l))))))

(defmfun apply2hack (rulelist e) 
  (prog (pairs max) 
     (setq max 0)
     (cond ((atom e) (return (cons (apply2 rulelist e -1) 0)))
	   ((specrepp e) (return (apply2hack rulelist (specdisrep e)))))
     (setq pairs (mapcar #'(lambda (x) (apply2hack rulelist x)) (cdr e)))
     (setq max 0)
     (mapc #'(lambda (l) (setq max (max max (cdr l)))) pairs)
     (setq e (simplifya (cons (delsimp (car e)) (mapcar #'car pairs)) t))
     (cond ((= max $maxapplyheight) (return (cons e max)))
	   (t (return (cons (apply2 rulelist e -1) (1+ max)))))))

;;;
;;; rpart.lisp code
;;; necessary to pass rtest_elliptic.mac
;;;
(defmfun $rectform (xx)
  (let ((ris (trisplit xx)))
    (add (car ris) (mul (cdr ris) '$%i))))

(defun trisplit (el) ; Top level of risplit
  (cond ((atom el) (risplit el))
	((specrepp el) (trisplit (specdisrep el)))
	((eq (caar el) 'mequal) (dot-sp-ri (cdr el) '(mequal simp)))
	(t (risplit el))))

(defun risplit-mplus (l)
  (do ((rpart) (ipart) (m (cdr l) (cdr m)))
      ((null m) (cons (addn rpart t) (addn ipart t)))
    (let ((sp (risplit (car m))))
      (cond ((=0 (car sp)))
	    (t (setq rpart (cons (car sp) rpart))))
      (cond ((=0 (cdr sp)))
	    (t (setq ipart (cons (cdr sp) ipart)))))))

(defun risplit-times (l)
  (let ((risl (do ((purerl nil)
		   (compl nil)
		   (l (cdr l) (cdr l)))
		  ((null l) (cons purerl compl))
		(let ((sp (risplit (car l))))
		  (cond ((=0 (cdr sp))
			 (setq purerl (rplacd sp purerl)))
			((or (atom (car sp)) (atom (cdr sp)))
			 (setq compl (cons sp compl)))
			((and (eq (caaar sp) 'mtimes)
;;;Try risplit z/w and notice denominator.  If this check were not made,
;;; the real and imaginary parts would not each be over a common denominator.
			      (eq (caadr sp) 'mtimes)
			      (let ((nr (nreverse (cdar sp)))
				    (ni (nreverse (cddr sp))))
				(cond ((equal (car nr) (car ni))
				       (push (car nr) purerl)
				       (push (cons (muln (nreverse (cdr nr)) t)
						   (muln (nreverse (cdr ni)) t))
					     compl))
				      (t
				       (setq nr (nreverse nr))
				       (setq ni (nreverse ni))
				       nil)))))
			(t
			 (push sp compl)))))))
    (cond ((null (cdr risl))
	   (cons (muln (car risl) t) 0))
	  (t
	   (do ((rpart 1) (ipart 0) (m (cdr risl) (cdr m)))
	       ((null m)
		(cons (muln (cons rpart (car risl)) t)
		      (muln (cons ipart (car risl)) t)))
	     (psetq rpart (sub (mul rpart (caar m)) (mul ipart (cdar m)))
		    ipart (add (mul ipart (caar m)) (mul rpart (cdar m)))))))))


(defun absarg1 (arg)
  (let ((arg1 arg) ($keepfloat t))
    (cond ((and (or (free arg '$%i)
		    (free (setq arg1 (sratsimp arg)) '$%i))
		(not (eq (csign arg1) t)))
	   (setq arg arg1)
	   (if implicit-real
	       (cons arg 0)
	       (unwind-protect
		    (prog2 (assume `(($notequal) ,arg 0))
			(absarg arg))
		 (forget `(($notequal) ,arg 0)))))
	  (t (absarg arg)))))

;;;	Main function
;;; Takes an expression and returns the dotted pair
;;; (<Real part> . <imaginary part>).

(defun risplit (l)
  (let (($domain '$complex) ($m1pbranch t) $logarc op)
    (cond ((atom l)
           ;; Symbols are assumed to represent real values, unless they have
           ;; been declared to be complex. If they have been declared to be both
           ;; real and complex, they are taken to be real.
	   (cond ((eq l '$%i) (cons 0 1))
		 ((eq l '$infinity) (cons '$und '$und))
		 ((and (decl-complexp l) (not (decl-realp l))) (risplit-noun l))
		 (t (cons l 0))))
	  ((eq (caar l) 'rat) (cons l 0))
	  ((eq (caar l) 'mplus) (risplit-mplus l))
	  ((eq (caar l) 'mtimes) (risplit-times l))
	  ((eq (caar l) 'mexpt) (risplit-expt l))
	  ((eq (caar l) '%log)
	   (let ((aa (absarg1 (cadr l))))
	     (rplaca aa (take '(%log) (car aa)))))
	  ((eq (caar l) 'bigfloat) (cons l 0)) ;All numbers are real.
	  ((and (member (caar l) '(%integrate %derivative %laplace %sum) :test #'eq)
		(freel (cddr l) '$%i))
	   (let ((ris (risplit (cadr l))))
	     (cons (simplify (list* (ncons (caar l)) (car ris) (cddr l)))
		   (simplify (list* (ncons (caar l)) (cdr ris) (cddr l))))))
          ((eq (caar l) '$conjugate)
           (cons (simplify (list '(%realpart) (cadr l)))
                 (mul -1 (simplify (list '(%imagpart) (cadr l))))))
	  ((let ((ass (assoc (caar l)
			     '((%sin %cosh %cos . %sinh)
			       (%cos %cosh %sin . %sinh)
			       (%sinh %cos %cosh . %sin)
			       (%cosh %cos %sinh . %sin)) :test #'eq)))
;;;This clause handles the very similar trigonometric and hyperbolic functions.
;;; It is driven by the table at the end of the lambda.
	     (and ass
		  (let ((ri (risplit (cadr l))))
		    (cond ((=0 (cdr ri)) ;Pure real case.
			   (cons (take (list (car ass)) (car ri)) 0))
			  (t
			   (cons (mul (take (list (car ass)) (car ri))
				      (take (list (cadr ass)) (cdr ri)))
				 (negate-if (eq (caar l) '%cos)
					    (mul (take (list (caddr ass)) (car ri))
						 (take (list (cdddr ass)) (cdr ri)))))))))))
	  ((member (caar l) '(%tan %tanh) :test #'eq)
	   (let ((sp (risplit (cadr l))))
;;;The similar tan and tanh cases.
	     (cond ((=0 (cdr sp))
		    (cons l 0))
		   (t
		    (let* ((2rl (mul (car sp) 2))
			   (2im (mul (cdr sp) 2))
			   (denom (inv (if (eq (caar l) '%tan)
					   (add (take '(%cosh) 2im) (take '(%cos) 2rl))
					   (add (take '(%cos) 2im) (take '(%cosh) 2rl))))))
		      (if (eq (caar l) '%tan)
			  (cons (mul (take '(%sin) 2rl) denom)
				(mul (take '(%sinh) 2im) denom))
			  (cons (mul (take '(%sinh) 2rl) denom)
				(mul (take '(%sin) 2im) denom))))))))
	  ((and (member (caar l) '(%atan %csc %sec %cot %csch %sech %coth) :test #'eq)
		(=0 (cdr (risplit (cadr l)))))
	   (cons l 0))
          ((and (eq (caar l) '$atan2)
                (not (zerop1 (caddr l)))
                (=0 (cdr (risplit (div (cadr l) (caddr l))))))
           ;; Case atan2(y,x) and y/x a real expression.
           (cons l 0))
	  ((or (arcp (caar l)) (eq (caar l) '$atan2))
	   (let ((ans (risplit (logarc (caar l) (cadr l)))))
	     (when (eq (caar l) '$atan2)
	       (setq ans (cons (sratsimp (car ans)) (sratsimp (cdr ans)))))
	     (if (and (free l '$%i) (=0 (cdr ans)))
		 (cons l 0)
		 ans)))
	  ((eq (caar l) '%plog)
	   ;;  (princ '|Warning: Principal value not guaranteed for Plog in Rectform/|)
	   (risplit (cons '(%log) (cdr l))))
	  ;; Look for a risplit-function on the property list to handle the
	  ;; realpart and imagpart for this function.
          ((setq op (safe-get (mop l) 'risplit-function))
	   (funcall op l))
;;; ^ All the above are guaranteed pure real.
;;; The handling of lists and matrices below has to be thought through.
	  ((eq (caar l) 'mlist) (dsrl l))
	  ((eq (caar l) '$matrix)
	   (dot--ri (mapcar #'dsrl (cdr l)) '($matrix simp)))
;;;The Coversinemyfoot clause covers functions which can be converted
;;; to functions known by risplit, such as the more useless trigonometrics.
	  ((let ((foot (coversinemyfoot l)))
	     (and foot (risplit foot))))
          ((or (safe-get (mop l) 'real-valued)
               (decl-realp (mop l)))
           ;; Simplification for a real-valued function
           (cons l 0))
          ((or (safe-get (mop l) 'commutes-with-conjugate)
               (safe-get (mop l) 'conjugate-function))
	   ;; A function with Mirror symmetry. The general expressions for
	   ;; the realpart and imagpart simplifies accordingly.
	   (cons (mul (div 1 2)
		      (add (simplify (list '($conjugate) l)) l))
		 (mul (div 1 2) '$%i
		      (sub (simplify (list '($conjugate) l)) l))))
;;; A MAJOR ASSUMPTION:
;;;  All random functions are pure real, regardless of argument.
;;;  This is evidently assumed by some of the integration functions.
;;;  Perhaps the best compromise is to return 'realpart/'imagpart
;;;   under the control of a switch set by the integrators.  First
;;;   all such dependencies must be found in the integ
	  ((and rp-polylogp (mqapplyp l) (eq (subfunname l) '$li)) (cons l 0))
	  ((prog2 (setq op (if (eq (caar l) 'mqapply) (caaadr l) (caar l)))
	       (decl-complexp op))
	   (risplit-noun l))
	  ((and (eq (caar l) '%product) (not (free (cadr l) '$%i)))
	   (risplit-noun l))
          (($subvarp l)
           ;; return a real answer for subscripted variable
           (cons l 0))
          (t
           (cons (list '(%realpart simp) l)
                 (list '(%imagpart simp) l))))))

;; absarg
;; returns pair (abs . arg)
;; if absflag is true, arg result is not guaranteed to be correct


;; The function of Absflag is to communicate that only the absolute
;; value part of the result is wanted.  This allows Absarg to avoid asking
;; questions irrelevant to the absolute value.  For instance, Cabs(x) is
;; invariably Abs(x), while the complex phase may be 0 or %pi.  Note also
;; the steps taken in Absarg to assure that Asksign's will happen before Sign's
;; as often as possible, so that, for instance, Abs(x) can be simplified to
;; x or -x if the sign of x must be known for some other reason.  These
;; techniques, however, are not perfect.

(defun absarg (l &optional (absflag nil))
;; Commenting out the the expansion of the expression l. It seems to be not
;; necessary, but can cause expression swelling (DK 01/2010).
;  (setq l ($expand l))
  (cond ((atom l)
	 (cond ((eq l '$%i)
		(cons 1 (simplify '((mtimes) ((rat simp) 1 2) $%pi))))
	       ((numberp l)
		(cons (abs l) (argnum l)))
	       ((member l '($%e $%pi) :test #'eq) (cons l 0))
	       ((eq l '$infinity) (cons '$inf '$ind))
               ((decl-complexp l)
                (cons (list '(mabs simp) l) ; noun form with mabs
                      (list '(%carg simp) l)))
	       (absflag (cons (take '(mabs) l) 0))
	       (t
                ;; At this point l is representing a real value. Try to
                ;; determine the sign and return a general form when the sign is
                ;; unknown.
		(let ((gs (if (eq rischp l) '$pos ($sign l))))
		  (cond ((member gs '($pos $pz)) (cons l 0))
			((eq gs '$zero) (cons 0 0))
			((eq gs '$neg)
			 (cons (neg l) (simplify '$%pi)))
			(t (cons (take '(mabs) l) (genatan 0 l))))))))
	((eq '$zero (let ((sign-imag-errp nil)) (catch 'sign-imag-err ($sign l))))
	 (cond ((some-bfloatp l)
		(cons bigfloatzero bigfloatzero))	; contagious
	       ((some-floatp l)
		(cons 0.0 0.0))
	       (t (cons 0 0))))
	((member (caar l) '(rat bigfloat) :test #'eq)
	 (cons (list (car l) (abs (cadr l)) (caddr l))
	       (argnum (cadr l))))
	((eq (caar l) 'mtimes)
	 (do ((n (cdr l) (cdr n))
	      (abars)
	      (argl () (cons (cdr abars) argl))
	      (absl () (rplacd abars absl)))
	     (())
	   (unless n
	     (return (cons (muln absl t) (2pistrip (addn argl t)))))
	   (setq abars (absarg (car n) absflag))))
        ((eq (caar l) 'mexpt)
         ;; An expression z^a
         (let ((aa (absarg (cadr l) nil)) ; (abs(z) . arg(z))
               (sp (risplit (caddr l)))   ; (realpart(a) . imagpart(a))
               ($radexpand nil))
           (cond ((and (zerop1 (cdr sp))
                       (eq ($sign (sub 1 (take '(mabs) (car sp)))) '$pos))
                  ;; Special case: a is real and abs(a) < 1.
                  ;; This simplifies e.g. carg(sqrt(z)) -> carg(z)/2
                  (cons (mul (power (car aa) (car sp))
                             (power '$%e (neg (mul (cdr aa) (cdr sp)))))
                        (mul (caddr l) (cdr aa))))
                 (t
                  ;; General case for z and a
                  (let ((arg (add (mul (cdr sp) (take '(%log) (car aa)))
                                  (mul (cdr aa) (car sp)))))
                    (cons (mul (power (car aa) (car sp))
                               (power '$%e (neg (mul (cdr aa) (cdr sp)))))
                          (if generate-atan2
			      (take '($atan2)
				    (take '(%sin) arg)
				    (take '(%cos) arg))
			    (take '(%atan) (take '(%tan) arg)))))))))
	((and (member (caar l) '(%tan %tanh) :test #'eq)
	      (not (=0 (cdr (risplit (cadr l))))))
	 (let* ((sp (risplit (cadr l)))
		(2frst (mul (cdr sp) 2))
		(2scnd (mul (car sp) 2)))
	   (when (eq (caar l) '%tanh)
	     (psetq 2frst 2scnd 2scnd 2frst))
	   (cons (let ((cosh (take '(%cosh) 2frst))
		       (cos (take '(%cos) 2scnd)))
		   (root (div (add cosh (neg cos))
			      (add cosh cos))
			 2))
		 (take '(%atan)
		       (if (eq (caar l) '%tan)
			   (div (take '(%sinh) 2frst) (take '(%sin) 2scnd))
			   (div (take '(%sin) 2frst) (take '(%sinh) 2scnd)))))))
	((specrepp l) (absarg (specdisrep l) absflag))
	((let ((foot (coversinemyfoot l)))
	   (and foot (not (=0 (cdr (risplit (cadr l))))) (absarg foot absflag))))
	(t
	 (let ((ris (trisplit l)))
	   (xcons
;;; Arguments must be in this order so that the side-effect of the Atan2,
;;; that is, determining the Asksign of the argument, can happen before
;;; Take Mabs does its Sign.  Blame JPG for noticing this lossage.
	    (if absflag 0 (genatan (cdr ris) (car ris)))
	    (cond ((equal (car ris) 0) (absarg-mabs (cdr ris)))
		  ((equal (cdr ris) 0) (absarg-mabs (car ris)))
		  (t (powers ($expand (add (powers (car ris) 2)
					   (powers (cdr ris) 2))
				      1 0)
			     (half)))))))))


;;; lesfac.lisp
;;; facrplus seems not working... redefined here.
(defun facrplus (x y)
  (let ((a (car x))
        (b (cdr x))
        (c (car y))
        (d (cdr y))
        dummy)
    (multiple-value-setq (x a c) (dopgcdcofacts a c))
    (multiple-value-setq (y b d) (fpgcdco b d))
    (setq a (makprod (pplus (pflatten (ptimeschk a d))
                            (pflatten (ptimeschk b c))) nil))
    (setq b (ptimeschk b d))
    (cond ($algebraic
           (setq y (ptimeschk y b))
           (multiple-value-setq (dummy y a) (fpgcdco y a)) ;for unexpected gcd
           (cons (ptimes x a) y))
          (t
           (multiple-value-setq (c y b) (cdinf y b nil))
           (multiple-value-setq (dummy y a) (fpgcdco y a))
           (cons (ptimes x a) (ptimeschk y (ptimeschk c b)))))))
