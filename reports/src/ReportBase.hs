module ReportBase
  ( module ReportBase
  , module LaTeXHelpers
  , module Text.LaTeX ) where

import Data.List (intersperse)
import Text.LaTeX hiding (titlepage)

import LaTeXHelpers

import qualified Identity.Student as Student
import qualified Identity.Institution as Institution

baseHeader :: LaTeXM ()
baseHeader = do
  documentclass [a4paper, "12pt", "table"] report
  usepackage [ "a4paper", "mag=1000"
             , "left=1.5cm", "right=1.5cm", "top=1.5cm", "bottom=1.5cm"
             , "headsep=0.7cm", "footskip=1cm" ] "geometry"
  -- Fonts
  usepackage [] "fontspec, unicode-math"
  setmainfont ["Ligatures=TeX"] "CMU Serif"
  setmonofont [] "CMU Typewriter Text"
  -- Russian language support
  usepackage ["english", "russian"] "babel"
  -- Proper quotes
  usepackage [] "upquote"
  -- Math
  usepackage ["fleqn"] "amsmath"
  usepackage [] "braket"
  -- tikz
  usepackage [] "tikz"
  -- Trees
  usepackage [] "qtree"
  -- Landscape orientation env
  usepackage [] "pdflscape"
   -- Always indent the first paragraph
  usepackage [] "indentfirst"
  -- Images
  usepackage [] "graphicx"
  -- Tables
  usepackage [] "multirow"
  usepackage [] "makecell"
  -- Alignment
  usepackage [] "adjustbox"

baseTitlePage :: (LaTeXM (), LaTeXM (), Maybe (LaTeXM ()), LaTeXM ()) -> LaTeXM ()
baseTitlePage (reportTitle, reportSubject, reportComment, reportYear) =
  multipleAuthorsTitlePage (reportTitle, reportSubject, reportComment, [Student.name], reportYear)

multipleAuthorsTitlePage :: (LaTeXM (), LaTeXM (), Maybe (LaTeXM ()), [LaTeXM ()], LaTeXM ()) -> LaTeXM ()
multipleAuthorsTitlePage (reportTitle, reportSubject, reportComment, reportAuthors, reportYear) =
  environment "titlepage" $ do
    center $ do
      textsc (Institution.name >> lnbreak (Mm 4) >> Institution.department)
      vfill
      textbf (reportTitle >> lnbreak (Mm 2) >> reportSubject >> optionalComment) >> lnbreak (Mm 20)
      mconcat (intersperse (lnbreak (Mm 2)) reportAuthors) >> lnbreak (Mm 2) >> Student.group
      vfill
      Institution.location >> lnbreak (Mm 2) >> reportYear
  where
    optionalComment = case reportComment of
      Just rComment -> lnbreak (Mm 4) >> rComment
      Nothing -> mempty
