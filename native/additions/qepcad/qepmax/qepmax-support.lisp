;;; qepcad-support.lisp
#|
Qepcad-support.lisp
Copyright (C) 2013, 2014 Yasuaki Honda

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
or the following URL: http://www.gnu.org/licenses/gpl-2.0.html.

You can contact one of the author by email:
Yasuaki dot Honda at Gmail dot Com
|#

(in-package :maxima)

(defvar *mtimes_char_maxima* (coerce "*" 'list))
(defvar *mtimes_char_qepcad* (coerce " " 'list))
(defvar *mtimes_char* *mtimes_char_maxima*)

(defun $prepare_for_qepcad ()
  (setf (get '%and 'strsym) (coerce " /\ " 'list))
  (setf (get '%or 'strsym) (coerce " \/ " 'list))
  (setq *mtimes_char* *mtimes_char_qepcad*)
  t)

(defun $restore_maxima ()
  (setf (get '%and 'strsym) nil)
  (setf (get '%or 'strsym) nil)
  (setq *mtimes_char* *mtimes_char_maxima*)
  t)

(defprop %and msize-nary grind)
(defprop %or msize-nary grind)

(defun msz-mtimes (x l r) (msznary x l r *mtimes_char*))

(def-led-equiv	$%and parse-nary)
(def-lbp	$%and 65.)
;RBP not needed
(def-pos	$%and $clause)
;RPOS not needed
(def-lpos	$%and $clause)

(def-led-equiv	$%or parse-nary)
(def-lbp	$%or 60.)
;RBP not needed
(def-pos	$%or $clause)
;RPOS not needed
(def-lpos	$%or $clause)


#|
qepcad lex elements:
variables: declared [a-zA-Z][a-zA-Z0-9]*
number: integer only [0-9]*
operator: 
The boolean operators are: /\ , \/ , ~ , ==> , <== , <==>. 
The relational operators are: = , /= , < , > , <= , >=
+, -, ^, (, ), [, ]
first letter:
[a-zA-Z] variables, or true false
[0-9] number
/ := /\ or /=
\ := \/
< := <==> or <== or <= or <
= := ==> or =
> := >= or >
+, -, ^, (, ), [, ],~
|#

(defvar *qepcad-prev-token* nil)

;;; qepcadlex1() is an internal function.
;;; interface function qepcadlex() will call this one.
;;; Three functions are implemented.
;;; 1. Insertion of "*" when num var or var var occurs.
;;; 2. operator /\ and \/ are re-written to be %and and %or
;;; 3. [ and ] are replaced to ( and ).
;;; 4. TRUE replaced with true, FALSE replaced with false.
(defun qepcadlex1 (ist ost) ;; ist must be an input stream
  (setq *qepcad-prev-token* nil)
  (let ((c))
    (while (not (equal (setq c (peek-char nil ist nil :eof))
		       :eof))
      (cond ((alpha-char-p c)
	     (let ((var (read-variable ist)))
	       (if (member *qepcad-prev-token*
			   '(:var :num))
		   (write-symbol " * " ost))
	       (setq *qepcad-prev-token* :var)
	       (write-symbol var ost)))
	    ((digit-char-p c)
	     (let ((num (read-num ist)))
	       (if (member *qepcad-prev-token*
			   '(:var :num))
		   (write-symbol " * " ost))
	       (setq *qepcad-prev-token* :num)
	       (write-symbol num ost)))
	    ((char= c #\/)
	     (let ((sym (read-slash ist)))
	       (setq *qepcad-prev-token* sym)
	       (if (string= sym "/\\")
		   (write-symbol "%and" ost)
		 (write-symbol sym ost))))
	    ((char= c #\\)
	     (let ((sym (read-bslash ist)))
	       (setq *qepcad-prev-token* sym)
	       (if (string= sym "\\/")
		   (write-symbol "%or" ost)
		 (write-symbol sym ost))))
	    ((char= c #\<)
	     (let ((sym (read-less ist)))
	       (setq *qepcad-prev-token* sym)
	       (write-symbol sym ost)))
	    ((char= c #\=) 
	     (let ((sym (read-equal ist)))
	       (setq *qepcad-prev-token* sym)
	       (write-symbol sym ost)))
	    ((char= c #\>)
	     (let ((sym (read-more ist)))
	       (setq *qepcad-prev-token* sym)
	       (write-symbol sym ost)))
	    ((char= c #\ )
	     (read-char ist)
	     (write-symbol " " ost))
	    ((single-char-token c)
	     (setq *qepcad-prev-token* (string c))
	     (write-symbol (string (read-char ist nil :eof)) ost))
	    (t (error (format nil "unknown char: ~A~&" c)))))))

(defun write-symbol (str ost)
  (cond ((string= str "[")
	 (setq str "("))
	((string= str "]")
	 (setq str ")"))
	((string= str "/=")
	 (setq str "#"))
	((string= str "TRUE")
	 (setq str "true"))
	((string= str "FALSE")
	 (setq str "false")))
  (format ost "~A" str))

(defun single-char-token (c)
  (find c "+-^()[]~" :test #'char=))

(defun read-variable (ist)
  (let ((out "")
	c)
    (while (and (characterp (setq c (peek-char nil ist nil :eof)))
		(or (alpha-char-p c) (digit-char-p c)))
      (read-char ist nil :eof)
      (setq out (concatenate 'string out (string c))))
    out))

(defun read-num (ist)
  (let ((out "")
	c)
    (while (and (characterp (setq c (peek-char nil ist nil :eof)))
		(digit-char-p c))
      (read-char ist nil :eof)
      (setq out (concatenate 'string out (string c))))
    out))

(defun read-slash (ist)
  (let ((out "") c)
    (setq out (concatenate 'string out (string (read-char ist))))
    (setq c (peek-char nil ist nil :eof))
    (if (find c "\\=" :test #'char=)
	(setq out (concatenate 'string out (string (read-char ist)))))
    out))

(defun read-bslash (ist)
  (let ((out "") c)
    (setq out (concatenate 'string out (string (read-char ist))))
    (setq c (peek-char nil ist nil :eof))
    (if (find c "/" :test #'char=)
	(setq out (concatenate 'string out (string (read-char ist)))))
    out))

(defun read-equal (ist)
  (let ((out "") c)
    (setq out (concatenate 'string out (string (read-char ist))))
    (setq c (peek-char nil ist nil :eof))
    (if (find c "=" :test #'char=)
	(progn
	  (setq out (concatenate 'string out (string (read-char ist))))
	  (setq c (peek-char nil ist nil :eof))
	  (if (find c ">" :test #'char=)
	      (setq out (concatenate 'string out (string (read-char ist))))
	    (unread-char #\= ist))))
    out))

(defun read-less (ist)
  (let ((out "") c)
    (setq out (concatenate 'string out (string (read-char ist))))
    (setq c (peek-char nil ist nil :eof))
    (if (find c "=" :test #'char=)
	(progn
	  (setq out (concatenate 'string out (string (read-char ist))))
	  (setq c (peek-char nil ist nil :eof))
	  (if (find c "=" :test #'char=)
	      (progn
		(setq out (concatenate 'string out (string (read-char ist))))
		(setq c (peek-char nil ist nil :eof))
		(if (find c ">" :test #'char=)
		    (progn
		      (setq out (concatenate 'string out (string (read-char ist))))
		      (setq c (peek-char nil ist nil :eof))))))))
    out))

(defun read-more (ist)
  (let ((out "") c)
    (setq out (concatenate 'string out (string (read-char ist))))
    (setq c (peek-char nil ist nil :eof))
    (if (char= c #\=)
	(setq out (concatenate 'string out (string (read-char ist)))))
    out))
      


(defun qepcadlex (str)
  (with-output-to-string (ost)
    (with-input-from-string (ist str)
       (qepcadlex1 ist ost))))
