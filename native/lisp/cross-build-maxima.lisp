;;; Cross compile Maxima with ECL for an Android target.
;;;
;;; Run from Maxima's src/ directory by the build machine's ECL (same version
;;; as the target ECL), after `configure --enable-ecl` and `make -C src bd`.
;;; See native/build.sh (stage_maxima).
;;;
;;; Maxima's own ECL build (BUILD-MAXIMA-LIB in maxima.system) compiles every
;;; file natively and loads the result so that later files see earlier macros.
;;; Here defsystem still drives the compile in dependency order, but each file
;;; is cross compiled to a target object (COMPILE-FILE :TARGET) and the source
;;; is loaded into the host instead of the binary.

(require 'cmp)

(defvar *cross-target* nil)
(defvar *cross-objects* '()
  "Target object files, in the order defsystem compiled them.")

(defun cross-compile-file (file &rest args &key output-file &allow-other-keys)
  (let ((object (compile-file-pathname output-file :system-p t)))
    (ensure-directories-exist object)
    ;; Like Maxima's DO-COMPILE-ECL, only a missing output file is fatal:
    ;; Maxima's sources compile with WARNINGs (e.g. deprecated defmvar options).
    (let ((truename (compile-file file :output-file object :system-p t
                                       :target *cross-target*)))
      (unless truename
        (error "Cross compilation of ~A failed" file))
      (push truename *cross-objects*)
      truename)))

(defun cross-build-maxima (target-info output)
  (setf *cross-target* (c:read-target-info target-info))
  (load "../lisp-utils/defsystem.lisp")
  (funcall (intern "ADD-REGISTRY-LOCATION" "MK") (truename "./"))
  ;; Loading maxima.system defines DO-COMPILE-ECL and names that symbol as
  ;; the compiler of every component; the symbol is only resolved when called,
  ;; so redefining it afterwards routes all compilation through us.
  (funcall (intern "FIND-SYSTEM" "MK") "maxima")
  (setf (fdefinition 'do-compile-ecl) #'cross-compile-file)
  (funcall (intern "OPERATE-ON-SYSTEM" "MK") "maxima" :compile
           :verbose nil
           :force :all
           :load-source-instead-of-binary t)
  ;; server.lisp does (require 'sb-bsd-sockets) at load time. A static
  ;; program cannot load modules at run time, so link the target's prebuilt
  ;; module libraries in front of Maxima's objects; initializing them
  ;; registers the modules and makes that REQUIRE a no-op.
  (let ((objects (append (mapcar (lambda (name) (merge-pathnames name target-info))
                                 '("libsockets.a" "libsb-bsd-sockets.a"))
                         (reverse *cross-objects*)))
        (ld-flags (let ((x (symbol-value (find-symbol "*AUTOCONF-LD-FLAGS*" "MAXIMA"))))
                    (if (and x (string/= x "")) (list x)))))
    (format t "~&;;; Linking ~D objects into ~A~%" (length objects) output)
    (with-compilation-unit (:target *cross-target*)
      (c:build-program output
                       :lisp-files objects
                       :ld-flags ld-flags
                       :epilogue-code '(progn (cl-user::run))))))
